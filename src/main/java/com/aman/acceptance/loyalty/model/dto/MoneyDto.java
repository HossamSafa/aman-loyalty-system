package com.aman.acceptance.loyalty.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoneyDto {

    @Schema(example = "1000.00")
    @NotNull(message = "amount.value is required")
    @Positive(message = "amount.value must be greater than zero")
    private BigDecimal value;

    @Schema(example = "EGP")
    @NotBlank(message = "amount.currency must not be blank")
    @Pattern(regexp = "[A-Z]{3}", message = "amount.currency must be exactly 3 uppercase letters")
    private String currency;
}