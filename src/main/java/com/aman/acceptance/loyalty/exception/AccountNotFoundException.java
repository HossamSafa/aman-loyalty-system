package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends LoyaltyException {

    public AccountNotFoundException(Long accountId) {
        super(
                ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Loyalty account not found for accountId: " + accountId,
                false
        );
    }
}