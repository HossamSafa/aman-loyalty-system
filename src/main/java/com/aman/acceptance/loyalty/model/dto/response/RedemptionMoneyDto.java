package com.aman.acceptance.loyalty.model.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RedemptionMoneyDto(
        @NotNull(message = "Value cannot be null")
        @Positive
        BigDecimal value,

        @NotBlank(message = "Currency cannot be blank")
        String currency
) {
    public RedemptionMoneyDto {
        if (currency == null || currency.isBlank()) {
            currency = "EGP";
        }
    }
}
