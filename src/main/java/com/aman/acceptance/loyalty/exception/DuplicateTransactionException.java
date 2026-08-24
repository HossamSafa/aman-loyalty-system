package com.aman.acceptance.loyalty.exception;

import org.springframework.http.HttpStatus;

public class DuplicateTransactionException extends LoyaltyException {

    public DuplicateTransactionException(String sourceTransactionId) {
        super(
                "LOYALTY_DUPLICATE_TRANSACTION",
                HttpStatus.CONFLICT,
                "Transaction already processed for sourceTransactionId: " + sourceTransactionId,
                false
        );
    }
}