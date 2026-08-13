package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.dto.request.ResolveCustomerRequest;
import com.aman.acceptance.loyalty.model.dto.response.ResolveCustomerResponse;
import com.aman.acceptance.loyalty.enums.ProgramStatus;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.mapper.CustomerMapper;
import com.aman.acceptance.loyalty.model.Customer;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import com.aman.acceptance.loyalty.repository.CustomerRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyProgramRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyProgramRepository loyaltyProgramRepository;
    private final CustomerMapper customerMapper;
    @Value("${loyalty.security.secret-key}")
    private String secretKey;

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    public CustomerService(CustomerRepository customerRepository, LoyaltyAccountRepository loyaltyAccountRepository,
                           LoyaltyProgramRepository loyaltyProgramRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.loyaltyProgramRepository = loyaltyProgramRepository;
        this.customerMapper = customerMapper;
    }


    private String normalizeMobile(String mobileNumber) {

        if (mobileNumber == null || mobileNumber.isBlank()) {
            return mobileNumber;
        }

        if (mobileNumber.startsWith("+20")) {
            return mobileNumber;
        }

        if (mobileNumber.startsWith("0")) {
            return "+20" + mobileNumber.substring(1);
        }

        if (mobileNumber.startsWith("20")) {
            return "+" + mobileNumber;
        }

        if (mobileNumber.startsWith("1")) {
            return "+20" + mobileNumber;
        }

        return mobileNumber;
    }

    private String maskMobile(String mobileNumber) {

        if (mobileNumber == null || mobileNumber.isBlank()) {
            return mobileNumber;
        }

        if (mobileNumber.length() < 7) {
            return mobileNumber;
        }

        return mobileNumber.substring(0, 5) + "******" + mobileNumber.substring(mobileNumber.length() - 2);
    }

    private String hashMobile(String normalizedMobile) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalizedMobile.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }

    private String encryptMobile(String normalizedMobile) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            byte[] encryptedBytes = cipher.doFinal(normalizedMobile.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }


    @Transactional
    public ResolveCustomerResponse resolve(ResolveCustomerRequest request, long programId) {

        String normalizeMobile = normalizeMobile(request.getMobileNumber());

        String mobileHash = hashMobile(normalizeMobile);

        Optional<Customer> existingCustomer = customerRepository.findByMobileHash(mobileHash);

        if (existingCustomer.isEmpty() && Boolean.FALSE.equals(request.getAutoEnroll())) {
            throw new LoyaltyException("LOYALTY_ACCOUNT_NOT_FOUND",
                    "No loyalty account exists and auto-enrollment was not requested.", HttpStatus.NOT_FOUND);
        }

        Customer customer;

        if (existingCustomer.isPresent()) {

            customer = existingCustomer.get();

        } else {

            customer = Customer.builder()
                    .mobileHash(mobileHash).mobileEncrypted(encryptMobile(normalizeMobile))
                    .name(request.getCustomerName()).build();

            customer = customerRepository.save(customer);
        }

        LoyaltyProgram program = loyaltyProgramRepository.findById(programId)
                .orElseThrow(() -> new LoyaltyException("LOYALTY_PROGRAM_NOT_FOUND",
                "The requested loyalty program was not found.", HttpStatus.NOT_FOUND));

        if (program.getStatus() == ProgramStatus.INACTIVE) {
            throw new LoyaltyException("LOYALTY_PROGRAM_INACTIVE",
                    "The merchant loyalty program is not active.", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        boolean newlyEnrolled = false;

        Optional<LoyaltyAccount> existingAccount = loyaltyAccountRepository.findByProgramAndCustomer(program, customer);

        LoyaltyAccount account;

        if (existingAccount.isEmpty() && Boolean.FALSE.equals(request.getAutoEnroll())) {
            throw new LoyaltyException("LOYALTY_ACCOUNT_NOT_FOUND",
                    "No loyalty account exists and auto-enrollment was not requested.", HttpStatus.NOT_FOUND);
        }


        if (existingAccount.isPresent()) {

            account = existingAccount.get();

        } else {

            account = LoyaltyAccount.builder().program(program).customer(customer).build();

            account = loyaltyAccountRepository.save(account);

            newlyEnrolled = true;

            log.info("Publishing event: loyalty.customer.enrolled.v1 for customerId={}, accountId={}",
                    customer.getId(), account.getId());
        }
        String mobileNumberMasked = maskMobile(normalizeMobile);
        return customerMapper.toResolveCustomerResponse(customer, account, newlyEnrolled, mobileNumberMasked);
    }


}
