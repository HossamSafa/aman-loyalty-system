package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.dto.BalanceDto;
import com.aman.acceptance.loyalty.dto.MoneyDto;
import com.aman.acceptance.loyalty.dto.request.RefundRequest;
import com.aman.acceptance.loyalty.dto.response.RefundResponse;
import com.aman.acceptance.loyalty.enums.*;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
    @RequiredArgsConstructor
    public class RefundService {

        private final LoyaltyTransactionRepository loyaltyTransactionRepository;
        private final PointsLotRepository pointsLotRepository;

        @Transactional
        public RefundResponse processRefund(RefundRequest request) {
            // 1. Check whether refundTransactionId was already processed
            boolean alreadyProcessed =
                    loyaltyTransactionRepository
                            .existsBySourceTransactionIdAndType(
                                    request.getRefundTransactionId(),
                                    TransactionType.REFUND_EARN_REVERSAL
                            );

            if (alreadyProcessed) {
                throw new IllegalStateException(
                        "Refund transaction was already processed: "
                                + request.getRefundTransactionId()
                );
            }
            // 2. Find the original EARN transaction
            LoyaltyTransaction originalTransaction =
                    loyaltyTransactionRepository
                            .findBySourceTransactionIdAndType(
                                    request.getOriginalTransactionId(),
                                    TransactionType.EARN
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Original earning transaction not found."
                                    ));

            // 3. Find the points lot created by that earning
            PointsLot pointsLot =
                    pointsLotRepository
                            .findByEarningTransactionId(
                                    originalTransaction.getId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Points lot not found."
                                    ));

            // 4. Validate that the lot is still LOCKED
            if (pointsLot.getStatus() != LotStatus.LOCKED) {
                throw new IllegalStateException(
                        "Points lot is not locked and cannot be processed by Flow 5."
                );
            }

            if (pointsLot.getRemainingPoints() <= 0) {
                throw new IllegalStateException(
                        "The points lot has no remaining points to reverse."
                );
            }
            // 5. Validate refund amount and currency
            BigDecimal originalSaleAmount = originalTransaction.getMoneyAmount();
            BigDecimal refundAmount = request.getRefundAmount().getValue();

            if (request.getRefundAmount().getCurrency() != CurrencyCode.EGP) {
                throw new IllegalArgumentException(
                        "Only EGP refunds are supported."
                );
            }

            if (originalSaleAmount == null) {
                throw new IllegalStateException(
                        "The original earning transaction has no sale amount."
                );
            }

            if (refundAmount.compareTo(originalSaleAmount) > 0) {
                throw new IllegalArgumentException(
                        "Refund amount cannot exceed the original sale amount."
                );
            }
            if (request.getRefundType() == RefundType.FULL
                    && refundAmount.compareTo(originalSaleAmount) != 0) {

                throw new IllegalArgumentException(
                        "A full refund amount must equal the original sale amount."
                );
            }

            if (request.getRefundType() == RefundType.PARTIAL
                    && refundAmount.compareTo(originalSaleAmount) >= 0) {

                throw new IllegalArgumentException(
                        "A partial refund amount must be less than the original sale amount."
                );
            }
            // 6. Calculate how many points must be reversed
            int reversedPoints;

            if (request.getRefundType() == RefundType.FULL) {

                reversedPoints = pointsLot.getRemainingPoints();

            } else {

                BigDecimal calculatedPoints =
                        BigDecimal.valueOf(originalTransaction.getPoints())
                                .multiply(refundAmount)
                                .divide(
                                        originalSaleAmount,
                                        0,
                                        RoundingMode.FLOOR
                                );


                reversedPoints = calculatedPoints.intValueExact();
            }
            if (reversedPoints <= 0) {
                throw new IllegalArgumentException(
                        "Refund amount is too small to reverse any points."
                );
            }

            if (reversedPoints > pointsLot.getRemainingPoints()) {
                throw new IllegalArgumentException(
                        "Refund would reverse more points than remain in the lot."
                );
            }

            // 7. Reduce the lot's remaining points
            int updatedRemainingPoints = pointsLot.getRemainingPoints() - reversedPoints;

            pointsLot.setRemainingPoints(updatedRemainingPoints);

            if (updatedRemainingPoints == 0) {
                pointsLot.setStatus(LotStatus.CANCELLED);
            }

            // 8. Reduce the account's locked balance
            LoyaltyAccount account = originalTransaction.getAccount();

            int updatedLockedPoints =
                    account.getLockedPoints() - reversedPoints;

            if (updatedLockedPoints < 0) {
                throw new IllegalStateException(
                        "Account locked balance cannot become negative."
                );
            }

            account.setLockedPoints(updatedLockedPoints);

            // 9. Create a REFUND_EARN_REVERSAL ledger transaction
            LoyaltyTransaction refundTransaction =
                    LoyaltyTransaction.builder()
                            .account(account)
                            .type(TransactionType.REFUND_EARN_REVERSAL)
                            .sourceTransactionId(request.getRefundTransactionId())
                            .points(-reversedPoints)
                            .moneyAmount(refundAmount)
                            .status(TransactionStatus.COMMITTED)
                            .build();

            loyaltyTransactionRepository.save(refundTransaction);


            // 10. Build and return RefundResponse
            int totalOwned =
                    account.getAvailablePoints()
                            + account.getLockedPoints()
                            + account.getReservedPoints();

            BalanceDto balance =
                    BalanceDto.builder()
                            .available(account.getAvailablePoints())
                            .locked(account.getLockedPoints())
                            .reserved(account.getReservedPoints())
                            .totalOwned(totalOwned)
                            .build();

            MoneyDto restoredRedemptionValue =
                    MoneyDto.builder()
                            .value(BigDecimal.ZERO)
                            .currency(request.getRefundAmount().getCurrency())
                            .build();

            return RefundResponse.builder()
                    .refundTransactionId(request.getRefundTransactionId())
                    .originalTransactionId(request.getOriginalTransactionId())
                    .status("COMPLETED")
                    .canceledLockedPoints(reversedPoints)
                    .reversedEarnedPoints(reversedPoints)
                    .restoredRedeemedPoints(0)
                    .restoredRedemptionValue(restoredRedemptionValue)
                    .balance(balance)
                    .build();



        }
    }
