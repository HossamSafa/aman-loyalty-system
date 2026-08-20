package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.enums.RedemptionStatus;

import java.time.LocalDateTime;

public record VerifyRedemptionResponseData(
        long redemptionId,
        RedemptionStatus status,
        String authorizationCode,
        Long reservedPoints,
        RedemptionMoneyDto discountAmount,
        LocalDateTime commitBefore
) {
}
