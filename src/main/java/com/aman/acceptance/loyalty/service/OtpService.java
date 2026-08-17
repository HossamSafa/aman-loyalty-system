package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.config.OtpProperties;
import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.exception.OtpInvalidException;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.Redemption;
import com.aman.acceptance.loyalty.model.dto.response.OtpMetadataDto;
import com.aman.acceptance.loyalty.util.PhoneMaskingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpProperties otpProperties;
    private final OtpNotificationService otpNotificationService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpMetadataDto initiate(LoyaltyAccount account, Redemption redemption) {

        String phoneNumber = account.getCustomer().getMobileEncrypted();

        String maskedPhone = PhoneMaskingUtil.maskPhoneNumber(phoneNumber);

        String otp = generateOtp(otpProperties.getLength());

        String otpHash = passwordEncoder.encode(otp);
        redemption.setOtpHash(otpHash);

        redemption.setOtpAttemptsRemaining(
                otpProperties.getMaxAttempts()
        );

        redemption.setOtpExpiresAt(
                LocalDateTime.now().plusSeconds(
                        otpProperties.getTtlSeconds()
                )
        );

        otpNotificationService.sendOtp(maskedPhone, otp);

        Instant expiresAt =
                Instant.now().plusSeconds(
                        otpProperties.getTtlSeconds()
                );

        return new OtpMetadataDto(
                maskedPhone,
                expiresAt,
                otpProperties.getMaxAttempts()
        );
    }

    public void verifyOtp(Redemption redemption, String otpInput) {
        if (redemption.getOtpExpiresAt() == null || LocalDateTime.now().isAfter(redemption.getOtpExpiresAt())) {
            throw LoyaltyException.unprocessable(ErrorCode.LOYALTY_OTP_EXPIRED, "OTP has expired.");
        }

        if (redemption.getOtpAttemptsRemaining() == null || redemption.getOtpAttemptsRemaining() <= 0) {
            throw LoyaltyException.tooManyRequests(ErrorCode.LOYALTY_OTP_ATTEMPTS_EXCEEDED,
                    "Maximum OTP verification attempts exceeded.");
        }

        if (redemption.getOtpHash() == null) {
            redemption.setOtpAttemptsRemaining(redemption.getOtpAttemptsRemaining() - 1);
            throw new OtpInvalidException( "Invalid OTP.");
        }

        if (!passwordEncoder.matches(otpInput, redemption.getOtpHash())) {
            redemption.setOtpAttemptsRemaining(redemption.getOtpAttemptsRemaining() - 1);
            throw new OtpInvalidException( "Invalid OTP.");
        }
    }

    public String generateAuthorizationCode() {
        int randomDigits = 100000 + secureRandom.nextInt(900000);
        return "LA-" + randomDigits;
    }

    private String generateOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}
