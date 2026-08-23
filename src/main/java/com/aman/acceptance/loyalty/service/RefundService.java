package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.*;
import com.aman.acceptance.loyalty.model.dto.BalanceDto;
import com.aman.acceptance.loyalty.model.dto.request.RefundRequest;
import com.aman.acceptance.loyalty.model.dto.response.MoneyResponseDto;
import com.aman.acceptance.loyalty.model.dto.response.RefundResponse;
import com.aman.acceptance.loyalty.enums.*;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.repository.RedemptionRepository;
import com.aman.acceptance.loyalty.service.calculation.RefundCalculation;
import com.aman.acceptance.loyalty.service.calculation.RefundCalculator;
import com.aman.acceptance.loyalty.service.validators.refundValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final LoyaltyTransactionRepository transactionRepository;
    private final PointsLotRepository pointsLotRepository;
    private final LoyaltyAccountRepository accountRepository;
    private final RefundCalculator refundCalculator;
    private final refundValidator refundValidator;
    private final RedemptionRepository redemptionRepository;
    private static final CurrencyCode SUPPORTED_CURRENCY = CurrencyCode.EGP;

    @Transactional
    public RefundResponse processRefund(RefundRequest request) {

        refundValidator.validateCurrency(request);
        LoyaltyTransaction existingRefund = findExistingRefund(request);
        if (existingRefund != null) {
            return buildExistingResponse(existingRefund, request);
        }

        LoyaltyTransaction originalTransaction = findOriginalTransaction(request.getOriginalTransactionId());

        LoyaltyAccount account = accountRepository.findByIdForUpdate(originalTransaction.getAccount().getId())
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                        "Loyalty account was not found."
                ));

        // Recheck after acquiring the lock — protects against concurrent duplicate refunds
        LoyaltyTransaction existingRefundAfterLock = findExistingRefund(request);
        if (existingRefundAfterLock != null) {
            return buildExistingResponse(existingRefundAfterLock, request);
        }

        PointsLot pointsLot = pointsLotRepository.findByEarningTransactionIdForUpdate(originalTransaction.getId())
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.POINTS_LOT_NOT_FOUND,
                        "Points lot was not found."
                ));

        refundValidator.validateLockedRefund(originalTransaction, pointsLot, request);

        List<LoyaltyTransaction> previousRefunds = transactionRepository
                .findAllByAccount_IdAndOriginalSourceTransactionIdAndTypeAndStatus(account.getId(),
                        request.getOriginalTransactionId(),
                        TransactionType.REFUND_EARN_REVERSAL,
                        TransactionStatus.COMMITTED
                );

        BigDecimal previouslyRefundedAmount = previousRefunds.stream()
                .map(LoyaltyTransaction::getMoneyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int previouslyReversedPoints = previousRefunds.stream()
                .mapToInt(transaction -> Math.negateExact(transaction.getPoints()))
                .reduce(0, Math::addExact);

        RefundCalculation calculation = refundCalculator.calculate(
                originalTransaction.getMoneyAmount(),
                originalTransaction.getPoints(),
                previouslyRefundedAmount,
                previouslyReversedPoints,
                request.getRefundAmount().getValue(),
                request.getRefundType()
        );

        int pointsToReverse = calculation.pointsToReverse();

        updatePointsLot(pointsLot, pointsToReverse);
        updateAccount(account, pointsToReverse);

        RedemptionRestoreResult restoration =
                restoreRedeemedPointsIfApplicable(request, account, originalTransaction, previouslyRefundedAmount);

        BalanceDto balance = buildBalance(account);

        LoyaltyTransaction refundTransaction =
                buildRefundTransaction(request, originalTransaction, account, pointsToReverse);

        transactionRepository.save(refundTransaction);

        return buildResponse(request, pointsToReverse, balance, restoration);
    }

    private LoyaltyTransaction findExistingRefund(RefundRequest request) {
        return transactionRepository
                .findBySourceTransactionIdAndType(
                        request.getRefundTransactionId(),
                        TransactionType.REFUND_EARN_REVERSAL
                )
                .orElse(null);
    }

    private LoyaltyTransaction findOriginalTransaction(String originalTransactionId) {
        return transactionRepository.findBySourceTransactionIdAndType(originalTransactionId, TransactionType.EARN)
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.ORIGINAL_TRANSACTION_NOT_FOUND,
                        "Original earning transaction was not found."
                ));
    }

    private void updatePointsLot(PointsLot pointsLot, int pointsToReverse) {
        int remainingPoints = Math.subtractExact(pointsLot.getRemainingPoints(), pointsToReverse);

        if (remainingPoints < 0) {
            throw BusinessException.conflict(
                    ErrorCode.REFUND_POINTS_EXCEEDED,
                    "Refund exceeds the remaining points."
            );
        }

        pointsLot.setRemainingPoints(remainingPoints);
        if (remainingPoints == 0) {
            pointsLot.setStatus(LotStatus.CANCELLED);
        }
    }

    private void updateAccount(LoyaltyAccount account, int pointsToReverse) {
        int lockedPoints = Math.subtractExact(account.getLockedPoints(), pointsToReverse);

        if (lockedPoints < 0) {
            throw BusinessException.conflict(
                    ErrorCode.NEGATIVE_LOCKED_BALANCE,
                    "Locked balance cannot become negative."
            );
        }

        account.setLockedPoints(lockedPoints);
    }

    private RedemptionRestoreResult restoreRedeemedPointsIfApplicable(RefundRequest request, LoyaltyAccount account,
                                                                      LoyaltyTransaction originalTransaction,
                                                                      BigDecimal previouslyRefundedAmount) {

        if (request.getRedemptionId() == null) {
            return RedemptionRestoreResult.none();
        }

        Redemption redemption = redemptionRepository.findByIdAndAccount_Id(request.getRedemptionId(), account.getId())
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.REDEMPTION_NOT_FOUND,
                        "Redemption was not found for this account."
                ));

        if (redemption.getStatus() != RedemptionStatus.COMMITTED) {
            throw BusinessException.conflict(
                    ErrorCode.LOYALTY_REDEMPTION_STATE_CONFLICT,
                    "Redemption is not committed and cannot be restored."
            );
        }

        if (!redemption.getPurchaseTransactionId().equals(originalTransaction.getSourceTransactionId())) {
            throw BusinessException.conflict(
                    ErrorCode.INVALID_REFUND_STATE,
                    "Redemption is not linked to the original refunded purchase."
            );
        }

        BigDecimal originalSaleAmount = originalTransaction.getMoneyAmount();

        BigDecimal cumulativeRefundAmount = previouslyRefundedAmount.add(request.getRefundAmount().getValue());

        int targetRestoredPoints;

        if (request.getRefundType() == RefundType.FULL) {
            targetRestoredPoints = redemption.getRequestedPoints();
        } else {
            targetRestoredPoints = BigDecimal.valueOf(redemption.getRequestedPoints())
                    .multiply(cumulativeRefundAmount)
                    .divide(originalSaleAmount, 0, RoundingMode.FLOOR)
                    .intValueExact();
        }

        int previouslyRestoredPoints = redemption.getAllocations().stream()
                .mapToInt(RedemptionAllocation::getRestoredPoints)
                .sum();

        int pointsToRestore = Math.subtractExact(targetRestoredPoints, previouslyRestoredPoints);

        if (pointsToRestore < 0) {
            throw BusinessException.conflict(
                    ErrorCode.INVALID_REFUND_STATE,
                    "Previously restored points exceed the expected restoration."
            );
        }

        restoreAllocationsProportionally(redemption, pointsToRestore);

        int newAvailablePoints = Math.addExact(account.getAvailablePoints(), pointsToRestore);
        account.setAvailablePoints(newAvailablePoints);

        BigDecimal restoredValue = redemption.getDiscountAmount()
                .multiply(BigDecimal.valueOf(pointsToRestore))
                .divide(BigDecimal.valueOf(redemption.getRequestedPoints()), 2, RoundingMode.FLOOR);

        return new RedemptionRestoreResult(pointsToRestore, restoredValue);
    }

    private void restoreAllocationsProportionally(Redemption redemption, int pointsToRestore) {

        int totalAllocatedPoints = redemption.getAllocations().stream()
                .mapToInt(RedemptionAllocation::getPoints)
                .sum();

        int remainingToDistribute = pointsToRestore;
        int allocationsCount = redemption.getAllocations().size();
        int index = 0;

        for (RedemptionAllocation allocation : redemption.getAllocations()) {
            index++;

            int pointsForThisAllocation;

            if (index == allocationsCount) {
                pointsForThisAllocation = remainingToDistribute;
            } else {
                pointsForThisAllocation = BigDecimal.valueOf(allocation.getPoints())
                        .multiply(BigDecimal.valueOf(pointsToRestore))
                        .divide(BigDecimal.valueOf(totalAllocatedPoints), 0, RoundingMode.FLOOR)
                        .intValueExact();
            }

            remainingToDistribute -= pointsForThisAllocation;

            // Track how many points have been restored for this specific allocation
            allocation.setRestoredPoints(Math.addExact(allocation.getRestoredPoints(), pointsForThisAllocation));

            PointsLot lot = allocation.getLot();
            lot.setRemainingPoints(Math.addExact(lot.getRemainingPoints(), pointsForThisAllocation));

            // A lot that was fully consumed (CANCELLED) becomes AVAILABLE again once it has remaining points
            if (lot.getStatus() == LotStatus.CANCELLED && lot.getRemainingPoints() > 0) {
                lot.setStatus(LotStatus.AVAILABLE);
            }
        }
    }

    private BalanceDto buildBalance(LoyaltyAccount account) {
        int totalOwned = Math.addExact(
                Math.addExact(account.getAvailablePoints(), account.getLockedPoints()),
                account.getReservedPoints()
        );

        return BalanceDto.builder()
                .available(account.getAvailablePoints())
                .locked(account.getLockedPoints())
                .reserved(account.getReservedPoints())
                .totalOwned(totalOwned)
                .build();
    }

    private LoyaltyTransaction buildRefundTransaction(RefundRequest request, LoyaltyTransaction originalTransaction,
                                                      LoyaltyAccount account, int reversedPoints) {
        return LoyaltyTransaction.builder()
                .account(account)
                .type(TransactionType.REFUND_EARN_REVERSAL)
                .sourceTransactionId(request.getRefundTransactionId())
                .idempotencyKey(request.getRefundTransactionId())
                .originalSourceTransactionId(originalTransaction.getSourceTransactionId())
                .refundType(request.getRefundType())
                .points(Math.negateExact(reversedPoints))
                .moneyAmount(request.getRefundAmount().getValue())
                .currency(request.getRefundAmount().getCurrency())
                .transactionTime(request.getRefundTime())
                .status(TransactionStatus.COMMITTED)
                .build();
    }

    private RefundResponse buildResponse(RefundRequest request, int reversedPoints, BalanceDto balance,
                                         RedemptionRestoreResult restoration) {
        return RefundResponse.builder()
                .refundTransactionId(request.getRefundTransactionId())
                .originalTransactionId(request.getOriginalTransactionId())
                .status(String.valueOf(RefundStatus.COMPLETED))
                .canceledLockedPoints(reversedPoints)
                .reversedEarnedPoints(reversedPoints)
                .restoredRedeemedPoints(restoration.restoredPoints())
                .restoredRedemptionValue(toMoneyResponseDto(restoration.restoredValue()))
                .balance(balance)
                .build();
    }

    private RefundResponse buildExistingResponse(LoyaltyTransaction existing, RefundRequest request) {
        refundValidator.validateSameRequest(existing, request);
        int reversedPoints = Math.negateExact(existing.getPoints());

        LoyaltyAccount account = existing.getAccount();
        BalanceDto balance = buildBalance(account);

        return RefundResponse.builder()
                .refundTransactionId(existing.getSourceTransactionId())
                .originalTransactionId(existing.getOriginalSourceTransactionId())
                .status(String.valueOf(RefundStatus.COMPLETED))
                .canceledLockedPoints(reversedPoints)
                .reversedEarnedPoints(reversedPoints)
                .restoredRedeemedPoints(0)
                .restoredRedemptionValue(zeroMoney())
                .balance(balance)
                .build();
    }

    private MoneyResponseDto zeroMoney() {
        return MoneyResponseDto.builder()
                .value(BigDecimal.ZERO.setScale(2))
                .currency(SUPPORTED_CURRENCY)
                .build();
    }

    private MoneyResponseDto toMoneyResponseDto(BigDecimal value) {
        return MoneyResponseDto.builder()
                .value(value)
                .currency(SUPPORTED_CURRENCY)
                .build();
    }

    private record RedemptionRestoreResult(Integer restoredPoints, BigDecimal restoredValue) {

        static RedemptionRestoreResult none() {
            return new RedemptionRestoreResult(0, BigDecimal.ZERO);
        }
    }
}