package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class AccountFrozenException extends LoyaltyException {

    public AccountFrozenException(Long accountId) {
        super(
                ErrorCode.LOYALTY_ACCOUNT_FROZEN,
                HttpStatus.LOCKED,
                "Loyalty account is frozen for accountId: " + accountId,
                false
        );
    }
}