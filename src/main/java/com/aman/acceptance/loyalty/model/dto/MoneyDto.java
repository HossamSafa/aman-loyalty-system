package com.aman.acceptance.loyalty.model.dto;

import com.aman.acceptance.loyalty.enums.CurrencyCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyDto {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal value;
    @NotNull(message = "Currency is required")
    private CurrencyCode currency;

}