package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.AdjustmentType;
import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.model.AuditEvent;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.request.AdjustmentRequest;
import com.aman.acceptance.loyalty.model.response.AdjustmentResponse;
import com.aman.acceptance.loyalty.repository.AuditEventRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.model.dto.BalanceDto;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdjustmentService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final PointsLotRepository pointsLotRepository;
    private final AuditEventRepository auditEventRepository;
    private final AccountFreezeService accountFreezeService;
    private final ObjectMapper objectMapper;

    @Value("${loyalty.rules.default-expiry-days}")
    private int defaultExpiryDays;

    @Transactional
    public AdjustmentResponse adjust(Long accountId, AdjustmentRequest request) {

        LoyaltyAccount account = loyaltyAccountRepository.findById(accountId)
                .orElseThrow(() -> LoyaltyException.notFound(
                        ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                        "No loyalty account exists with id: " + accountId
                ));

        accountFreezeService.assertAccountActive(account);

        String beforeJson = buildSnapshotJson(account);

        LoyaltyTransaction transaction = (request.getType() == AdjustmentType.CREDIT)
                ? applyCredit(account, request)
                : applyDebit(account, request);

        loyaltyAccountRepository.save(account);

        String afterJson = buildSnapshotJson(account);

        AuditEvent auditEvent = saveAuditEvent(
                request.getActorId(),
                "ADJUSTMENT_" + request.getType().name(),
                accountId,
                beforeJson,
                afterJson
        );

        log.info("Account [{}] {} adjustment of {} points by actor [{}] - reason: {}",
                accountId, request.getType(), request.getPoints(), request.getActorId(), request.getReasonCode());

        return AdjustmentResponse.builder()
                .adjustmentId(transaction.getId())
                .loyaltyTransactionId(transaction.getId())
                .type(request.getType().name())
                .points(transaction.getPoints())
                .balance(BalanceDto.builder()
                        .available(account.getAvailablePoints())
                        .locked(account.getLockedPoints())
                        .reserved(account.getReservedPoints())
                        .totalOwned(account.getTotalOwned())
                        .build())
                .auditId(auditEvent.getId())
                .build();
    }

    // Credit

    private LoyaltyTransaction applyCredit(LoyaltyAccount account, AdjustmentRequest request) {

        int expiryDays = (request.getExpiresInDays() != null) ? request.getExpiresInDays() : defaultExpiryDays;

        String adjustmentReference = "adj-" + UUID.randomUUID();

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .account(account)
                .type(TransactionType.ADJUSTMENT_CREDIT)
                .sourceTransactionId(adjustmentReference)
                .idempotencyKey(adjustmentReference)
                .points(request.getPoints())
                .transactionTime(LocalDateTime.now())
                .build();
        transaction = loyaltyTransactionRepository.save(transaction);

        PointsLot lot = PointsLot.builder()
                .account(account)
                .earningTransaction(transaction)
                .originalPoints(request.getPoints())
                .remainingPoints(request.getPoints())
                .unlockAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(expiryDays))
                .status(LotStatus.AVAILABLE)
                .build();
        pointsLotRepository.save(lot);

        account.setAvailablePoints(account.getAvailablePoints() + request.getPoints());

        return transaction;
    }

    // Debit

    private LoyaltyTransaction applyDebit(LoyaltyAccount account, AdjustmentRequest request) {

        if (account.getAvailablePoints() < request.getPoints()) {
            throw LoyaltyException.unprocessable(
                    ErrorCode.LOYALTY_INSUFFICIENT_AVAILABLE_POINTS,
                    "The account does not have enough spendable points."
            );
        }

        consumeLotsFifo(account, request.getPoints());

        String adjustmentReference = "adj-" + UUID.randomUUID();

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .account(account)
                .type(TransactionType.ADJUSTMENT_DEBIT)
                .sourceTransactionId(adjustmentReference)
                .idempotencyKey(adjustmentReference)
                .points(-request.getPoints())
                .transactionTime(LocalDateTime.now())
                .build();
        transaction = loyaltyTransactionRepository.save(transaction);

        account.setAvailablePoints(account.getAvailablePoints() - request.getPoints());

        return transaction;
    }

    private void consumeLotsFifo(LoyaltyAccount account, int pointsToConsume) {
        List<PointsLot> availableLots =
                pointsLotRepository.findByAccountAndStatusOrderByExpiresAtAsc(account, LotStatus.AVAILABLE);

        int remaining = pointsToConsume;
        for (PointsLot lot : availableLots) {
            if (remaining <= 0) break;
            if (lot.getRemainingPoints() <= 0) continue;

            int consumeFromThisLot = Math.min(lot.getRemainingPoints(), remaining);
            lot.setRemainingPoints(lot.getRemainingPoints() - consumeFromThisLot);
            remaining -= consumeFromThisLot;
            pointsLotRepository.save(lot);
        }

        if (remaining > 0) {
            log.error("Data inconsistency for account [{}]: availablePoints allowed a debit of {} points, "
                            + "but only {} points were actually found across available lots.",
                    account.getId(), pointsToConsume, pointsToConsume - remaining);

            throw LoyaltyException.internal(
                    ErrorCode.LOYALTY_DATA_INCONSISTENCY,
                    "Unable to source the requested points from available lots for account " + account.getId()
            );
        }
    }

    // Helpers
    private AuditEvent saveAuditEvent(String actorId, String action, Long accountId, String beforeJson, String afterJson) {
        AuditEvent auditEvent = AuditEvent.builder()
                .actorId(actorId)
                .action(action)
                .entityType("LOYALTY_ACCOUNT")
                .entityId(accountId)
                .beforeJson(beforeJson)
                .afterJson(afterJson)
                .correlationId(UUID.randomUUID().toString())
                .build();
        return auditEventRepository.save(auditEvent);
    }

    private String buildSnapshotJson(LoyaltyAccount account) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("accountId", account.getId());
        snapshot.put("status", account.getStatus().name());
        snapshot.put("availablePoints", account.getAvailablePoints());
        snapshot.put("lockedPoints", account.getLockedPoints());
        snapshot.put("reservedPoints", account.getReservedPoints());

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.warn("Failed to serialize snapshot for account [{}]", account.getId(), e);
            return "{}";
        }
    }
}