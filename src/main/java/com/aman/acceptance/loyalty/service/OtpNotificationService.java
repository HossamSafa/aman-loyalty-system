package com.aman.acceptance.loyalty.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OtpNotificationService {

    public void sendOtp(String phoneNumber, String otp) {
        log.info("OTP generated for {}: {}", phoneNumber, otp);
    }

}
