package com.aman.acceptance.loyalty.service.validators;

import com.aman.acceptance.loyalty.dto.request.RefundRequest;
import com.aman.acceptance.loyalty.enums.CurrencyCode;
import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.enums.RefundType;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class refundValidator {
    private static final CurrencyCode SUPPORTED_CURRENCY = CurrencyCode.EGP;


    public void validateSameRequest(LoyaltyTransaction existing, RefundRequest request) {
        boolean sameRequest =
                existing.getOriginalSourceTransactionId()
                        .equals(request.getOriginalTransactionId())
                        && existing.getRefundType()
                        == request.getRefundType()
                        && existing.getMoneyAmount()
                        .compareTo(
                                request.getRefundAmount().getValue()
                        ) == 0
                        && existing.getCurrency()
                        == request.getRefundAmount().getCurrency();

        if (!sameRequest) {
            throw BusinessException.conflict(
                    ErrorCode.REFUND_IDEMPOTENCY_CONFLICT,
                    "Refund transaction ID was used with different data."
            );
        }
    }
    public void validateLockedRefund(LoyaltyTransaction originalTransaction, PointsLot pointsLot, RefundRequest request) {
        if (pointsLot.getStatus() != LotStatus.LOCKED) {
            throw BusinessException.conflict(
                    ErrorCode.POINTS_NOT_LOCKED,
                    "Earned points are no longer locked."
            );
        }

        if (!request.getRefundTime()
                .isBefore((pointsLot.getUnlockAt()))) {

            throw BusinessException.conflict(
                    ErrorCode.REFUND_WINDOW_EXPIRED,
                    "Refund occurred outside the locked window."
            );
        }

        if (originalTransaction.getMoneyAmount() == null
                || originalTransaction.getMoneyAmount().signum() <= 0) {

            throw BusinessException.conflict(
                    ErrorCode.INVALID_ORIGINAL_AMOUNT,
                    "Original transaction has an invalid sale amount."
            );
        }

        if (originalTransaction.getPoints() == null
                || originalTransaction.getPoints() <= 0) {

            throw BusinessException.conflict(
                    ErrorCode.INVALID_ORIGINAL_POINTS,
                    "Original transaction has invalid earned points."
            );
        }
    }
    public void validateCurrency(
            RefundRequest request
    ) {
        if (request.getRefundAmount().getCurrency()
                != SUPPORTED_CURRENCY) {

            throw BusinessException.invalid(
                    ErrorCode.UNSUPPORTED_CURRENCY,
                    "Only EGP refunds are supported."
            );
        }
    }

    public void validateRefundType(RefundType refundType, int amountComparison) {
        if (refundType == RefundType.FULL
                && amountComparison != 0) {

            throw BusinessException.invalid(
                    ErrorCode.INVALID_FULL_REFUND,
                    "A full refund must complete the original sale amount."
            );
        }

        if (refundType == RefundType.PARTIAL
                && amountComparison >= 0) {

            throw BusinessException.invalid(
                    ErrorCode.INVALID_PARTIAL_REFUND,
                    "A partial refund must be less than the remaining sale amount."
            );
        }
    }
}
