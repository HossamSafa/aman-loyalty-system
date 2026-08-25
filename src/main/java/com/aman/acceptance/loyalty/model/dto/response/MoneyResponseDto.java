package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.enums.CurrencyCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyResponseDto {

    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal value;

    private CurrencyCode currency;
}