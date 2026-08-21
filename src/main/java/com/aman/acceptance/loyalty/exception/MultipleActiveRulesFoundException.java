package com.aman.acceptance.loyalty.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class MultipleActiveRulesFoundException extends LoyaltyException {

    public MultipleActiveRulesFoundException(Long programId, LocalDateTime transactionTime) {
        super(
                "LOYALTY_RULE_CONFIGURATION_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Multiple active loyalty rules found for programId: "
                        + programId
                        + " at transactionTime: "
                        + transactionTime,
                false
        );
    }
}
