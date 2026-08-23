package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class ProgramInactiveException extends LoyaltyException {

    public ProgramInactiveException(Long programId) {
        super(
                ErrorCode.LOYALTY_PROGRAM_INACTIVE,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "The merchant loyalty program is not active: " + programId,
                false
        );
    }
}