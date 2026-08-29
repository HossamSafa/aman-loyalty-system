//package com.aman.acceptance.loyalty.util;
//
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//
//public class MobileHashUtil {
//
//    public static String normalizeMobile(String mobileNumber) {
//        if (mobileNumber == null || mobileNumber.isBlank())
//            return mobileNumber;
//        if (mobileNumber.startsWith("+20"))
//            return mobileNumber;
//        if (mobileNumber.startsWith("0"))
//            return "+20" + mobileNumber.substring(1);
//        if (mobileNumber.startsWith("20"))
//            return "+" + mobileNumber;
//        if (mobileNumber.startsWith("1"))
//            return "+20" + mobileNumber;
//        return mobileNumber;
//    }
//
//    public static String hashMobile(String normalizedMobile) {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hashBytes = digest.digest(normalizedMobile.getBytes(StandardCharsets.UTF_8));
//            StringBuilder hexString = new StringBuilder();
//            for (byte b : hashBytes) {
//                hexString.append(String.format("%02x", b));
//            }
//            return hexString.toString();
//        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException("Hashing algorithm not available", e);
//        }
//    }
//}
