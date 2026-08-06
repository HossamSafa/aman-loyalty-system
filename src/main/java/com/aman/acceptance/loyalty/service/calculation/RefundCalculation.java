package com.aman.acceptance.loyalty.service.calculation;

import java.math.BigDecimal;

public record RefundCalculation(
        BigDecimal cumulativeRefundAmount,
        int pointsToReverse
) {
}
