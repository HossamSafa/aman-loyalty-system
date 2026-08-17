package com.aman.acceptance.loyalty.utilies;


import com.aman.acceptance.loyalty.exception.CredentialsException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public  class MobileNumberMasker {

//    public static String mask(String mobileNumber) {
//        if (mobileNumber == null || mobileNumber.isBlank()) {
//            return null;
//        }
//
//        if (mobileNumber.length() <= 6) {
//            return "******";
//        }
//
//        int visibleDigits = 4;
//        int maskedLength = mobileNumber.length() - visibleDigits;
//        return mobileNumber.substring(0, 4)
//                + "*".repeat(Math.max(0, maskedLength - 4))
//                + mobileNumber.substring(mobileNumber.length() - 2);
//    }

            public static String hashMobileNumber(final String mobileNumber) throws CredentialsException {
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
        }






