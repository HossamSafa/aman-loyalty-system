package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.enums.RedemptionStatus;

public record CommitResponseData(
        String redemptionId,
        RedemptionStatus status,
        Long redeemedPoints,
        RedemptionMoneyDto discountAmount,
        String loyaltyTransactionId,
        BalanceDto balance
) {}
