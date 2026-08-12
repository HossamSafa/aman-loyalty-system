package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LoyaltyException extends RuntimeException{

    private final ErrorCode code;
    private final HttpStatus status;
    private final boolean retryable;

    public LoyaltyException(ErrorCode code, String message, HttpStatus status, boolean retryable) {
        super(message);
        this.code = code;
        this.status = status;
        this.retryable = retryable;
    }



    public static LoyaltyException notFound(ErrorCode code, String message) {
        return new LoyaltyException(code, message, HttpStatus.NOT_FOUND, false);
    }

    public static LoyaltyException conflict(ErrorCode code, String message) {
        return new LoyaltyException(code, message, HttpStatus.CONFLICT, false);
    }

    public static LoyaltyException locked(ErrorCode code, String message) {
        return new LoyaltyException(code, message, HttpStatus.LOCKED, false);
    }

    public static LoyaltyException badRequest(ErrorCode code, String message) {
        return new LoyaltyException(code, message, HttpStatus.BAD_REQUEST, false);
    }

    public static LoyaltyException internal(ErrorCode code, String message) {
        return new LoyaltyException(code, message, HttpStatus.INTERNAL_SERVER_ERROR, true);
    }
    public static LoyaltyException unprocessable(ErrorCode code, String message) {
        return new LoyaltyException(code, message, HttpStatus.UNPROCESSABLE_ENTITY, false);
    }

    public static LoyaltyException tooManyRequests(ErrorCode code, String message) {
        return new LoyaltyException(code, message, HttpStatus.TOO_MANY_REQUESTS, false);
    }
}


