package com.aman.acceptance.loyalty.exception;

import org.springframework.http.HttpStatus;

public class AccountFrozenException extends LoyaltyException {

    public AccountFrozenException(Long accountId) {
        super(
                "LOYALTY_ACCOUNT_FROZEN",
                HttpStatus.LOCKED,
                "Loyalty account is frozen for accountId: " + accountId,
                false
        );
    }
}