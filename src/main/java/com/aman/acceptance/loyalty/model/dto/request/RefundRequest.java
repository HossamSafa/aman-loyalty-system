package com.aman.acceptance.loyalty.model.dto.request;

import com.aman.acceptance.loyalty.model.dto.MoneyDto;
import com.aman.acceptance.loyalty.enums.RefundType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest
{   @NotBlank (message = "Refund transaction ID is required")
    private String refundTransactionId;
    @NotBlank(message = "Original transaction ID is required")
    private String originalTransactionId;
    private Long redemptionId;
    @NotNull(message = "Refund type is required")
    private RefundType refundType;
    @NotNull(message = "Refund amount is required")
    @Valid
    private MoneyDto refundAmount;
    @NotNull(message = "Refund time is required")
    private LocalDateTime refundTime;
}
