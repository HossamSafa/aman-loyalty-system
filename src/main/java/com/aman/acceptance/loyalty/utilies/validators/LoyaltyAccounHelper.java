package com.aman.acceptance.loyalty.utilies.validators;

import com.aman.acceptance.loyalty.exception.CredentialsException;
import com.aman.acceptance.loyalty.exception.ResourceNotFoundException;
import com.aman.acceptance.loyalty.model.responses.ConversionResponse;
import com.aman.acceptance.loyalty.model.responses.MetaResponse;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
@Component
@RequiredArgsConstructor
public class LoyaltyAccounHelper {
    @Value("${loyalty.conversion.conversionOfMoney}")
    private BigDecimal conversionOfMoney;

    @Value("${loyalty.conversion.rateOfPoint}")
    private BigDecimal rateOfPoint;

    private final LoyaltyAccountRepository accountRepository;

    public  void validateAccountExists(final Long accountId) {
/**
 * this is gard if
 */
        if (!accountRepository.existsById(accountId)) {

            throw new ResourceNotFoundException("Loyalty account not found: " + accountId);}

    }

    public static String mobileNumberMasked(final String mobileNumber) throws CredentialsException {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            throw new CredentialsException("mobileNumber cant be null");
        }

        byte[] hash;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            hash = messageDigest.digest(mobileNumber.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new CredentialsException("algorethim not found");
        }
        return Base64.getEncoder().encodeToString(hash);
    }

    public ConversionResponse buildConversion() {
        return new ConversionResponse(conversionOfMoney,rateOfPoint);
    }

    public static MetaResponse buildMeta(final String correlationId) {
        return new MetaResponse(correlationId,Instant.now());
    }

}
