package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.enums.RedemptionStatus;

public record RedemptionResponseData(
        long redemptionId,
        RedemptionStatus status,
        Long reservedPoints,
        RedemptionMoneyDto discountAmount,
        RedemptionMoneyDto payableAfterDiscount,
        OtpMetadataDto otp
) {
}
