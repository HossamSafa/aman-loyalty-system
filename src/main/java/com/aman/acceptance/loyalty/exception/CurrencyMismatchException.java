package com.aman.acceptance.loyalty.exception;

import org.springframework.http.HttpStatus;

public class CurrencyMismatchException extends LoyaltyException {

    public CurrencyMismatchException(
            Long programId,
            String programCurrency,
            String requestCurrency
    ) {
        super(
                "LOYALTY_CURRENCY_MISMATCH",
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Transaction currency ("
                        + requestCurrency
                        + ") does not match program currency ("
                        + programCurrency
                        + ") for programId: "
                        + programId,
                false
        );
    }
}
