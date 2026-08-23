package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends LoyaltyException {

    public IdempotencyConflictException(String idempotencyKey) {
        super(
                ErrorCode.LOYALTY_IDEMPOTENCY_CONFLICT,
                HttpStatus.CONFLICT,
                "Idempotency key was already used with a different request body: "
                        + idempotencyKey,
                false
        );
    }
}