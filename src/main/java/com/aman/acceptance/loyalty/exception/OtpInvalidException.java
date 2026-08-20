package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;


public class OtpInvalidException extends LoyaltyException {

    public OtpInvalidException(String message) {
        super(
                ErrorCode.LOYALTY_OTP_INVALID,
                message,
                HttpStatus.UNPROCESSABLE_ENTITY,
                false
        );
    }
}
