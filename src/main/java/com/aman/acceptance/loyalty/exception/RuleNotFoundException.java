package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class RuleNotFoundException extends LoyaltyException {

    public RuleNotFoundException(Long programId, LocalDateTime transactionTime) {
        super(
                ErrorCode.LOYALTY_RULE_NOT_FOUND,
                HttpStatus.UNPROCESSABLE_CONTENT,
                "No effective loyalty rule found for programId: "
                        + programId
                        + " at transactionTime: "
                        + transactionTime,
                false
        );
    }
}