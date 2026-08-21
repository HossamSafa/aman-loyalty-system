package com.aman.acceptance.loyalty.exception;

import org.springframework.http.HttpStatus;

public class ProgramInactiveException extends LoyaltyException {

    public ProgramInactiveException(Long programId) {
        super(
                "LOYALTY_PROGRAM_INACTIVE",
                HttpStatus.UNPROCESSABLE_ENTITY,
                "The merchant loyalty program is not active: " + programId,
                false
        );
    }
}