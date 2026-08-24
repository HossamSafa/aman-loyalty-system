package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.enums.CurrencyCode;
import com.aman.acceptance.loyalty.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class CurrencyMismatchException extends LoyaltyException {

    public CurrencyMismatchException(
            Long programId,
            CurrencyCode programCurrency,
            CurrencyCode requestCurrency
    ) {
        super(
                ErrorCode.LOYALTY_CURRENCY_MISMATCH,
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