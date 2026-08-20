package com.aman.acceptance.loyalty.model.dto.request;

import com.aman.acceptance.loyalty.model.dto.response.RedemptionMoneyDto;
import com.aman.acceptance.loyalty.enums.RedeemMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RedemptionRequest(
    @NotNull(message = "Account ID cannot be null")
    Long accountId,

        @NotBlank(message = "Purchase Transaction ID cannot be blank")
        String purchaseTransactionId,

        @NotNull(message = "Purchase Amount cannot be null")
        @Valid
    RedemptionMoneyDto purchaseAmount,

        @NotNull(message = "Redeem Mode cannot be null")
        RedeemMode redeemMode,

        @NotNull(message = "Requested points cannot be null")
        @Positive(message = "Requested points must be positive")
        Long requestedPoints
) {
}
