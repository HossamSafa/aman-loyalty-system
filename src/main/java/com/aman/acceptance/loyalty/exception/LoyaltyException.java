package com.aman.acceptance.loyalty.exception;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LoyaltyException extends RuntimeException{
    private final String code;
    private final HttpStatus status;
    private final boolean retryable;
    private final Object details;

    public LoyaltyException(
            String code,
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

    public LoyaltyException(
            String code,
            HttpStatus status,
            String message,
            boolean retryable
    ) {
        this(code, status, message, retryable, null);
    }

}
