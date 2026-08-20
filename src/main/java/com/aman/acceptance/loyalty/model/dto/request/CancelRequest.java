package com.aman.acceptance.loyalty.model.dto.request;

import com.aman.acceptance.loyalty.enums.RedemptionCancelReason;
import jakarta.validation.constraints.NotNull;

public record CancelRequest(
        @NotNull(message = "Cancel reason cannot be null")
        RedemptionCancelReason reason
) {}
