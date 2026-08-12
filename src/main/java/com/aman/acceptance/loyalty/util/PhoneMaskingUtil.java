package com.aman.acceptance.loyalty.util;

public class PhoneMaskingUtil {

    private PhoneMaskingUtil() {
        // Utility class
    }

    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return phoneNumber;
        }

        int prefixLength = 5;
        int suffixLength = 2;

        if (phoneNumber.length() <= prefixLength + suffixLength) {
            return phoneNumber;
        }

        String prefix = phoneNumber.substring(0, prefixLength);
        String suffix = phoneNumber.substring(phoneNumber.length() - suffixLength);
        String maskedMiddle = "*".repeat(phoneNumber.length() - prefixLength - suffixLength);

        return prefix + maskedMiddle + suffix;
    }
}
