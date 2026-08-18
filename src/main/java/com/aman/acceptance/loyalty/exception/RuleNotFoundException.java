package com.aman.acceptance.loyalty.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class RuleNotFoundException extends LoyaltyException {

    public RuleNotFoundException(Long programId, LocalDateTime transactionTime) {
        super(
                "LOYALTY_RULE_NOT_FOUND",
                HttpStatus.UNPROCESSABLE_CONTENT,                "No effective loyalty rule found for programId: "
                        + programId
                        + " at transactionTime: "
                        + transactionTime,
                false
        );
    }
}