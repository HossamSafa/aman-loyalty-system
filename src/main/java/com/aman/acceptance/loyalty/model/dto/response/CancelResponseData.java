package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.enums.RedemptionStatus;

public record CancelResponseData(
        String redemptionId,
        RedemptionStatus status,
        Long releasedPoints,
        BalanceDto balance
) {}
