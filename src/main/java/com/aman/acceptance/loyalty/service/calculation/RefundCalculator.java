package com.aman.acceptance.loyalty.service.calculation;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.enums.RefundType;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.service.validators.refundValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class RefundCalculator {
private  final refundValidator refundValidator;
    public RefundCalculation calculate(BigDecimal originalSaleAmount, int originalEarnedPoints, BigDecimal previouslyRefundedAmount, int previouslyReversedPoints, BigDecimal currentRefundAmount, RefundType refundType) {
        BigDecimal cumulativeRefundAmount =
                previouslyRefundedAmount.add(currentRefundAmount);

        int amountComparison =
                cumulativeRefundAmount.compareTo(originalSaleAmount);

        if (amountComparison > 0) {
            throw BusinessException.invalid(
                    ErrorCode.REFUND_AMOUNT_EXCEEDED,
                    "Total refund amount cannot exceed the original sale amount."
            );
        }

        refundValidator.validateRefundType(refundType, amountComparison);

        int targetReversedPoints;

        if (amountComparison == 0) {
            /*
             * When the sale becomes fully refunded, reverse all original
             * points to remove any remaining rounding difference.
             */
            targetReversedPoints = originalEarnedPoints;
        } else {
            targetReversedPoints =
                    BigDecimal.valueOf(originalEarnedPoints)
                            .multiply(cumulativeRefundAmount)
                            .divide(
                                    originalSaleAmount,
                                    0,
                                    RoundingMode.FLOOR
                            )
                            .intValueExact();
        }

        int pointsToReverse =
                Math.subtractExact(
                        targetReversedPoints,
                        previouslyReversedPoints
                );

        if (pointsToReverse < 0) {
            throw BusinessException.conflict(
                    ErrorCode.INVALID_REFUND_STATE,
                    "Previously reversed points exceed the expected refund points."
            );
        }

        return new RefundCalculation(
                cumulativeRefundAmount,
                pointsToReverse
        );
    }


}

///The RefundCalculator class is preferable to placing the calculation logic inside RefundService because it separates the business calculation logic from the workflow/orchestration logic, following the Single Responsibility Principle (SRP).
///
/// Benefits
///
/// 1. Single Responsibility
///
/// Instead of having RefundService responsible for:
///
/// Retrieving data from the database.
/// Validating the input.
/// Calculating refund points.
/// Updating the account.
/// Creating the transaction.
///
/// It is now responsible only for orchestrating the workflow, while RefundCalculator is solely responsible for performing the business calculations.