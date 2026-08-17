package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.dto.request.CustomerRequestDto;
import com.aman.acceptance.loyalty.model.dto.response.CustomerDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.aman.acceptance.loyalty.util.MobileUtil;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyProgramRepository loyaltyProgramRepository;
    private final CustomerMapper customerMapper;
    private final MobileUtil mobileUtil;

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    public CustomerService(CustomerRepository customerRepository, LoyaltyAccountRepository loyaltyAccountRepository,
                           LoyaltyProgramRepository loyaltyProgramRepository, CustomerMapper customerMapper,
                           MobileUtil mobileUtil) {
        this.customerRepository = customerRepository;
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.loyaltyProgramRepository = loyaltyProgramRepository;
        this.customerMapper = customerMapper;
        this.mobileUtil = mobileUtil;
    }


    private Customer getOrCreateCustomer(String mobileHash, String normalizedMobile, String customerName,
                                         Boolean autoEnroll) {

        Optional<Customer> existingCustomer = customerRepository.findByMobileHash(mobileHash);

        if (existingCustomer.isEmpty() && Boolean.FALSE.equals(autoEnroll)) {
            throw new LoyaltyException("LOYALTY_ACCOUNT_NOT_FOUND",
                    "No loyalty account exists and auto-enrollment was not requested.", HttpStatus.NOT_FOUND);
        }

        if (existingCustomer.isPresent()) {
            return existingCustomer.get();
        }

        Customer customer = Customer.builder()
                .mobileHash(mobileHash).mobileEncrypted(mobileUtil.encryptMobile(normalizedMobile))
                .name(customerName).build();

        return customerRepository.save(customer);
    }

    private record AccountResolution(LoyaltyAccount account, boolean newlyEnrolled) {}

    private AccountResolution getOrCreateAccount(LoyaltyProgram program, Customer customer, Boolean autoEnroll) {

        Optional<LoyaltyAccount> existingAccount = loyaltyAccountRepository.findByProgramAndCustomer(program, customer);

        if (existingAccount.isEmpty() && Boolean.FALSE.equals(autoEnroll)) {
            throw new LoyaltyException("LOYALTY_ACCOUNT_NOT_FOUND",
                    "No loyalty account exists and auto-enrollment was not requested.", HttpStatus.NOT_FOUND);
        }

        if (existingAccount.isPresent()) {
            return new AccountResolution(existingAccount.get(), false);
        }

        LoyaltyAccount account = new LoyaltyAccount(program, customer);
        account = loyaltyAccountRepository.save(account);

        log.info("Publishing event: loyalty.customer.enrolled.v1 for customerId={}, accountId={}",
                customer.getId(), account.getId());

        return new AccountResolution(account, true);
    }

    @Transactional
    public CustomerDto resolve(CustomerRequestDto request, long programId) {

        String normalizeMobile = mobileUtil.normalizeMobile(request.getMobileNumber());

        String mobileHash = mobileUtil.hashMobile(normalizeMobile);

        Customer customer = getOrCreateCustomer(mobileHash, normalizeMobile, request.getCustomerName(),
                request.getAutoEnroll());

        LoyaltyProgram program = loyaltyProgramRepository.findById(programId)
                .orElseThrow(() -> new LoyaltyException("LOYALTY_PROGRAM_NOT_FOUND",
                "The requested loyalty program was not found.", HttpStatus.NOT_FOUND));

        if (program.getStatus() == ProgramStatus.INACTIVE) {
            throw new LoyaltyException("LOYALTY_PROGRAM_INACTIVE",
                    "The merchant loyalty program is not active.", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        AccountResolution accountResolution = getOrCreateAccount(program, customer, request.getAutoEnroll());

        LoyaltyAccount account = accountResolution.account();
        boolean newlyEnrolled = accountResolution.newlyEnrolled();

        String mobileNumberMasked = mobileUtil.maskMobile(normalizeMobile);
        return customerMapper.toResolveCustomerResponse(customer, account, newlyEnrolled, mobileNumberMasked);
    }

}
