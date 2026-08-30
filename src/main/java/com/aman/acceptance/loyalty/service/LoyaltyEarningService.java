package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.exception.AccountException;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.request.EarningRequest;
import com.aman.acceptance.loyalty.model.responses.BalanceResponse;
import com.aman.acceptance.loyalty.model.responses.EarningResponse;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aman.acceptance.loyalty.enums.TransactionStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class LoyaltyEarningService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final PointsLotRepository pointsLotRepository;

    @Transactional
    public EarningResponse earnPoints(
            final EarningRequest earningRequest,
            final String idempotencyKey,
            final String correlationId
    )throws AccountException {

        final var existingByIdempotencyKey = loyaltyTransactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingByIdempotencyKey.isPresent()) {

            return createEarningResponseFromTransaction(existingByIdempotencyKey.get());

        }

        final var existingTransaction =
               loyaltyTransactionRepository.findBySourceTransactionId(earningRequest.getSourceTransactionId());

        if (existingTransaction.isPresent()) {
            return createEarningResponseFromTransaction(existingTransaction.get());
        }

        validateRequest(earningRequest);

        final LoyaltyAccount account =

                loyaltyAccountRepository.findByIdForUpdate(Long.valueOf(earningRequest.getAccountId()))

                        .orElseThrow(() -> new AccountException("Loyalty account not found"));

        final long earnedPoints = calculatePoints(earningRequest.getAmount().getValue());

        final OffsetDateTime transactionTime = earningRequest.getTransactionTime();

        final OffsetDateTime unlockAt = transactionTime.plusDays(30);

        final OffsetDateTime expiresAt = transactionTime.plusDays(360);

        final LoyaltyTransaction transaction = new LoyaltyTransaction();

        transaction.setAccount(account);

        transaction.setSourceTransactionId(earningRequest.getSourceTransactionId());

        transaction.setTransactionTime(transactionTime.toLocalDateTime());

        transaction.setStatus(TransactionStatus.COMMITTED);

        transaction.setType(TransactionType.EARN);

        transaction.setPoints((int) earnedPoints);

        transaction.setIdempotencyKey(idempotencyKey);

        transaction.setRuleVersion(4);

        loyaltyTransactionRepository.save(transaction);

         final PointsLot pointsLot = new PointsLot();

        pointsLot.setAccount(account);

        pointsLot.setEarningTransaction(transaction);

        pointsLot.setOriginalPoints((int) earnedPoints);

        pointsLot.setRemainingPoints((int) earnedPoints);

        pointsLot.setUnlockAt(unlockAt.toLocalDateTime());

        pointsLot.setExpiresAt(expiresAt.toLocalDateTime());

        pointsLot.setStatus(LotStatus.LOCKED);

        pointsLotRepository.save(pointsLot);

        account.setLockedPoints(account.getLockedPoints() + (int) earnedPoints);

        loyaltyAccountRepository.save(account);

        return createEarningResponseFromTransaction(transaction, pointsLot, account);

    }

    private static long calculatePoints(final BigDecimal amount) {

        return amount
                .multiply(BigDecimal.valueOf(200))
                .setScale(0, RoundingMode.FLOOR)
                .longValue();
    }

    private static void validateRequest(final EarningRequest earningRequest) throws NoSuchElementException {

        if (earningRequest.getAmount() == null) {

            throw new NoSuchElementException("Amount is required");
        }

        if (earningRequest.getAmount().getValue() == null) {

            throw new NoSuchElementException("Amount value is required");

        }

        if (earningRequest.getAmount().getValue().compareTo(BigDecimal.ZERO) <= 0) {

            throw new NoSuchElementException ("Amount must be greater than zero");

        }

        if (!"EGP".equalsIgnoreCase(earningRequest.getAmount().getCurrency())) {

            throw new NoSuchElementException("Currency must be EGP");

        }
    }

    private EarningResponse createEarningResponseFromTransaction(
            final LoyaltyTransaction transaction,
            final PointsLot pointsLot,
            final LoyaltyAccount account) {

        final BalanceResponse balance =
                new BalanceResponse(
                        account.getAvailablePoints(),
                        account.getLockedPoints(),
                        account.getReservedPoints(),
                        account.getAvailablePoints() + account.getLockedPoints() + account.getReservedPoints());

        return new EarningResponse(
                transaction.getId().toString(),
                transaction.getSourceTransactionId(),
                (long) pointsLot.getOriginalPoints(),
                pointsLot.getStatus().name(),
                pointsLot.getUnlockAt().atOffset(ZoneOffset.UTC),
                pointsLot.getExpiresAt().atOffset(ZoneOffset.UTC),
                transaction.getRuleVersion(),
                balance
        );
    }

    private EarningResponse createEarningResponseFromTransaction(
            final LoyaltyTransaction transaction) throws NoSuchElementException {

        final LoyaltyAccount account = transaction.getAccount();

        final PointsLot pointsLot =

                pointsLotRepository.findByEarningTransactionIdForUpdate(transaction.getId())

                        .orElseThrow(() -> new NoSuchElementException("Points lot not found"));

        return createEarningResponseFromTransaction(transaction, pointsLot, account);

    }

}