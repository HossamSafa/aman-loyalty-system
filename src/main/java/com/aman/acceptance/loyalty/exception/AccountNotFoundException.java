package com.aman.acceptance.loyalty.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends LoyaltyException {

    public AccountNotFoundException(Long accountId) {
        super(
                "LOYALTY_ACCOUNT_NOT_FOUND",
                HttpStatus.NOT_FOUND,
                "Loyalty account not found for accountId: " + accountId,
                false
        );
    }
}