package com.aman.acceptance.loyalty.service;
import com.aman.acceptance.loyalty.model.dto.BalanceDto;
import com.aman.acceptance.loyalty.model.dto.request.RefundRequest;
import com.aman.acceptance.loyalty.model.dto.response.MoneyResponseDto;
import com.aman.acceptance.loyalty.model.dto.response.RefundResponse;
import com.aman.acceptance.loyalty.enums.*;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.service.calculation.RefundCalculation;
import com.aman.acceptance.loyalty.service.calculation.RefundCalculator;
import com.aman.acceptance.loyalty.service.validators.refundValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final LoyaltyTransactionRepository transactionRepository;
    private final PointsLotRepository pointsLotRepository;
    private final LoyaltyAccountRepository accountRepository;
    private final RefundCalculator refundCalculator;
    private final refundValidator refundValidator;
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
                        .orElseThrow(() ->
                                BusinessException.notFound(
                                        ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                                        "Loyalty account was not found."
                                )
                        );

        PointsLot pointsLot = pointsLotRepository.findByEarningTransactionIdForUpdate(originalTransaction.getId())
                        .orElseThrow(() ->
                                BusinessException.notFound(
                                        ErrorCode.POINTS_LOT_NOT_FOUND,
                                        "Points lot was not found."
                                )
                        );

        refundValidator .validateLockedRefund(originalTransaction, pointsLot, request);

        List<LoyaltyTransaction> previousRefunds = transactionRepository
                        .findAllByAccount_IdAndOriginalSourceTransactionIdAndTypeAndStatus(account.getId(),
                                request.getOriginalTransactionId(),
                                TransactionType.REFUND_EARN_REVERSAL,
                                TransactionStatus.COMMITTED
                        );

        BigDecimal previouslyRefundedAmount =
                previousRefunds.stream()
                        .map(LoyaltyTransaction::getMoneyAmount)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        int previouslyReversedPoints =
                previousRefunds.stream()
                        .mapToInt(transaction ->
                                Math.negateExact(
                                        transaction.getPoints()
                                )
                        )
                        .reduce(
                                0,
                                Math::addExact
                        );

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

        BalanceDto balance = buildBalance(account);

        LoyaltyTransaction refundTransaction =
                buildRefundTransaction(request, originalTransaction, account, pointsToReverse);

        transactionRepository.save(refundTransaction);

        return buildResponse(request, pointsToReverse, balance);
    }

    private LoyaltyTransaction findExistingRefund(
            RefundRequest request
    ) {
        return transactionRepository
                .findBySourceTransactionIdAndType(
                        request.getRefundTransactionId(),
                        TransactionType.REFUND_EARN_REVERSAL
                )
                .orElse(null);
    }

    private LoyaltyTransaction findOriginalTransaction(String originalTransactionId) {
        return transactionRepository.findBySourceTransactionIdAndType(originalTransactionId, TransactionType.EARN)
                .orElseThrow(() ->
                        BusinessException.notFound(
                                ErrorCode.ORIGINAL_TRANSACTION_NOT_FOUND,
                                "Original earning transaction was not found."
                        )
                );
    }
    private void updatePointsLot(PointsLot pointsLot, int pointsToReverse) {
        int remainingPoints =
                Math.subtractExact(
                        pointsLot.getRemainingPoints(),
                        pointsToReverse
                );

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
        int lockedPoints =
                Math.subtractExact(
                        account.getLockedPoints(),
                        pointsToReverse
                );

        if (lockedPoints < 0) {
            throw BusinessException.conflict(
                    ErrorCode.NEGATIVE_LOCKED_BALANCE,
                    "Locked balance cannot become negative."
            );
        }

        account.setLockedPoints(lockedPoints);
    }

    private BalanceDto buildBalance(
            LoyaltyAccount account
    ) {
        int totalOwned =
                Math.addExact(
                        Math.addExact(
                                account.getAvailablePoints(),
                                account.getLockedPoints()
                        ),
                        account.getReservedPoints()
                );

        return BalanceDto.builder()
                .available(account.getAvailablePoints())
                .locked(account.getLockedPoints())
                .reserved(account.getReservedPoints())
                .totalOwned(totalOwned)
                .build();
    }

    private LoyaltyTransaction buildRefundTransaction(RefundRequest request, LoyaltyTransaction originalTransaction, LoyaltyAccount account, int reversedPoints) {
        return LoyaltyTransaction.builder()
                .account(account)
                .type(TransactionType.REFUND_EARN_REVERSAL)
                .sourceTransactionId(request.getRefundTransactionId())
                .originalSourceTransactionId(originalTransaction.getSourceTransactionId())
                .refundType(request.getRefundType())
                .points(Math.negateExact(reversedPoints))
                .moneyAmount(request.getRefundAmount().getValue())
                .currency(request.getRefundAmount().getCurrency())
                .transactionTime(request.getRefundTime())
                .status(TransactionStatus.COMMITTED)
                .build();
    }

    private RefundResponse buildResponse(RefundRequest request, int reversedPoints, BalanceDto balance) {
        return RefundResponse.builder()
                .refundTransactionId(request.getRefundTransactionId())
                .originalTransactionId(request.getOriginalTransactionId())
                .status(String.valueOf(RefundStatus.COMPLETED))
                .canceledLockedPoints(reversedPoints)
                .reversedEarnedPoints(reversedPoints)
                .restoredRedeemedPoints(0)
                .restoredRedemptionValue(zeroMoney())
                .balance(balance)
                .build();
    }

    private RefundResponse buildExistingResponse(LoyaltyTransaction existing, RefundRequest request) {
        refundValidator.validateSameRequest(existing, request);
        int reversedPoints =
                Math.negateExact(existing.getPoints());

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
}