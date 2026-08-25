package com.aman.acceptance.loyalty.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class MobileUtil {

    @Value("${loyalty.security.secret-key}")
    private String secretKey;

    public String normalizeMobile(String mobileNumber) {

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

    public String maskMobile(String mobileNumber) {

        if (mobileNumber == null || mobileNumber.isBlank()) {
            return mobileNumber;
        }

        if (mobileNumber.length() < 7) {
            return mobileNumber;
        }

        return mobileNumber.substring(0, 5) + "******" + mobileNumber.substring(mobileNumber.length() - 2);
    }

    public String hashMobile(String normalizedMobile) {
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

    public String encryptMobile(String normalizedMobile) {
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

}
