package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.config.OtpProperties;
import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.exception.OtpInvalidException;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.Redemption;
import com.aman.acceptance.loyalty.model.dto.response.OtpMetadataDto;
import com.aman.acceptance.loyalty.util.MobileUtil;
import com.aman.acceptance.loyalty.util.PhoneMaskingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpProperties otpProperties;
    private final OtpNotificationService otpNotificationService;
    private final PasswordEncoder passwordEncoder;
    private final MobileUtil mobileUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpMetadataDto initiate(LoyaltyAccount account, Redemption redemption) {
        log.info("[LOYALTY] START OtpService.initiate | redemptionId={}", redemption.getId());

        String encryptedPhoneNumber = account.getCustomer().getMobileEncrypted();

        String decryptedPhoneNumber = mobileUtil.decryptMobile(encryptedPhoneNumber);
        String maskedPhone = PhoneMaskingUtil.maskPhoneNumber(decryptedPhoneNumber);

        String otp = generateOtp(otpProperties.getLength());
        log.info("[LOYALTY] OTP generated | redemptionId={} | length={}", redemption.getId(), otp.length());

        String otpHash = passwordEncoder.encode(otp);
        redemption.setOtpHash(otpHash);
        log.info("[LOYALTY] OTP hash stored | redemptionId={}", redemption.getId());

        redemption.setOtpAttemptsRemaining(
                otpProperties.getMaxAttempts()
        );

        redemption.setOtpExpiresAt(
                LocalDateTime.now().plusSeconds(
                        otpProperties.getTtlSeconds()
                )
        );
        log.info("[LOYALTY] OTP expiration set | redemptionId={} | expiresAt={}", redemption.getId(), redemption.getOtpExpiresAt());

        otpNotificationService.sendOtp(maskedPhone, otp);
        log.info("[LOYALTY] OTP notification sent | redemptionId={}", redemption.getId());

        Instant expiresAt =
                Instant.now().plusSeconds(
                        otpProperties.getTtlSeconds()
                );

        log.info("[LOYALTY] END OtpService.initiate | redemptionId={}", redemption.getId());
        return new OtpMetadataDto(
                maskedPhone,
                expiresAt,
                otpProperties.getMaxAttempts(),
                otp
        );
    }

    public void verifyOtp(Redemption redemption, String otpInput) {
        log.info("[LOYALTY] START OtpService.verifyOtp | redemptionId={}", redemption.getId());
        if (redemption.getOtpExpiresAt() == null || LocalDateTime.now().isAfter(redemption.getOtpExpiresAt())) {
            log.warn("[LOYALTY] OTP validation failed - expired | redemptionId={}", redemption.getId());
            throw LoyaltyException.unprocessable(ErrorCode.LOYALTY_OTP_EXPIRED, "OTP has expired.");
        }

        if (redemption.getOtpAttemptsRemaining() == null || redemption.getOtpAttemptsRemaining() <= 0) {
            log.warn("[LOYALTY] OTP validation failed - attempts exceeded | redemptionId={}", redemption.getId());
            throw LoyaltyException.tooManyRequests(ErrorCode.LOYALTY_OTP_ATTEMPTS_EXCEEDED,
                    "Maximum OTP verification attempts exceeded.");
        }

        if (redemption.getOtpHash() == null) {
            redemption.setOtpAttemptsRemaining(redemption.getOtpAttemptsRemaining() - 1);
            log.warn("[LOYALTY] OTP invalid | redemptionId={} | attemptsRemaining={}", redemption.getId(), redemption.getOtpAttemptsRemaining());
            throw new OtpInvalidException( "Invalid OTP.");
        }

        if (!passwordEncoder.matches(otpInput, redemption.getOtpHash())) {
            redemption.setOtpAttemptsRemaining(redemption.getOtpAttemptsRemaining() - 1);
            log.warn("[LOYALTY] OTP invalid | redemptionId={} | attemptsRemaining={}", redemption.getId(), redemption.getOtpAttemptsRemaining());
            throw new OtpInvalidException( "Invalid OTP.");
        }
        log.info("[LOYALTY] OTP verification succeeded | redemptionId={}", redemption.getId());
        log.info("[LOYALTY] END OtpService.verifyOtp | redemptionId={}", redemption.getId());
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
