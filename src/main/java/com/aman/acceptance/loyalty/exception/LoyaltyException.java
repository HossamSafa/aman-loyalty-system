package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LoyaltyException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;
    private final boolean retryable;
    private final Object details;

    public LoyaltyException(
            ErrorCode code,
            HttpStatus status,
            String message,
            boolean retryable,
            Object details
    ) {
        super(message);
        this.code = code;
        this.status = status;
        this.retryable = retryable;
        this.details = details;
    }

    protected LoyaltyException(
            ErrorCode code,
            HttpStatus status,
            String message,
            boolean retryable
    ) {
        this(code, status, message, retryable, null);
    }

    public static LoyaltyException notFound(ErrorCode code, String message) {
        return new LoyaltyException(code, HttpStatus.NOT_FOUND, message, false, null);
    }

    public static LoyaltyException conflict(ErrorCode code, String message) {
        return new LoyaltyException(code, HttpStatus.CONFLICT, message, false, null);
    }

    public static LoyaltyException locked(ErrorCode code, String message) {
        return new LoyaltyException(code, HttpStatus.LOCKED, message, false, null);
    }

    public static LoyaltyException invalid(ErrorCode code, String message) {
        return new LoyaltyException(code, HttpStatus.UNPROCESSABLE_ENTITY, message, false, null);
    }

    public static LoyaltyException badRequest(ErrorCode code, String message) {
        return new LoyaltyException(code, HttpStatus.BAD_REQUEST, message, false, null);
    }

    public static LoyaltyException internal(
            ErrorCode code,
            String message,
            HttpStatus status,
            boolean retryable
    ) {
        return new LoyaltyException(code, status, message, retryable, null);
    }
}