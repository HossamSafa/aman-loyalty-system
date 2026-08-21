package com.aman.acceptance.loyalty.model.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarningRequest {
    @Schema(example = "4451")
    @NotNull(message = "accountId is required")
    private Long accountId;

    @Schema(example = "sale-20260727-00091")
    @NotBlank(message = "sourceTransactionId must not be blank")
    private String sourceTransactionId;

    @NotNull(message = "amount is required")
    @Valid
    private MoneyDto amount;

    @Schema(example = "2026-07-27T07:15:20")
    @NotNull(message = "transactionTime is required")
    private LocalDateTime transactionTime;

    @Schema(example = "POS")
    @NotBlank(message = "channel must not be blank")
    private String channel;
}
