package com.aman.acceptance.loyalty.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LoyaltyException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public LoyaltyException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
