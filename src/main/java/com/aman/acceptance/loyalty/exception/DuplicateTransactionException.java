package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class DuplicateTransactionException extends LoyaltyException {

    public DuplicateTransactionException(String sourceTransactionId) {
        super(
                ErrorCode.LOYALTY_DUPLICATE_TRANSACTION,
                HttpStatus.CONFLICT,
                "Transaction already processed for sourceTransactionId: " + sourceTransactionId,
                false
        );
    }
}