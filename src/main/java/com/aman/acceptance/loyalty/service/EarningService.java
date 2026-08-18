package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.AccountStatus;
import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.enums.ProgramStatus;
import com.aman.acceptance.loyalty.enums.RuleStatus;
import com.aman.acceptance.loyalty.enums.TransactionStatus;
import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.event.PointsEarnedEvent;
import com.aman.acceptance.loyalty.exception.AccountFrozenException;
import com.aman.acceptance.loyalty.exception.AccountNotFoundException;
import com.aman.acceptance.loyalty.exception.CurrencyMismatchException;
import com.aman.acceptance.loyalty.exception.DuplicateTransactionException;
import com.aman.acceptance.loyalty.exception.IdempotencyConflictException;
import com.aman.acceptance.loyalty.exception.MultipleActiveRulesFoundException;
import com.aman.acceptance.loyalty.exception.ProgramInactiveException;
import com.aman.acceptance.loyalty.exception.RuleNotFoundException;
import com.aman.acceptance.loyalty.model.AuditEvent;
import com.aman.acceptance.loyalty.model.IdempotencyRecord;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.RuleVersion;
import com.aman.acceptance.loyalty.model.dto.BalanceDto;
import com.aman.acceptance.loyalty.model.dto.EarningRequest;
import com.aman.acceptance.loyalty.model.dto.EarningResponse;
import com.aman.acceptance.loyalty.repository.AuditEventRepository;
import com.aman.acceptance.loyalty.repository.IdempotencyRecordRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.repository.RuleVersionRepository;
import com.aman.acceptance.loyalty.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EarningService {

    private static final String ENDPOINT = "POST /api/v1/loyalty/earnings";

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final RuleVersionRepository ruleVersionRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final PointsLotRepository pointsLotRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public EarningService(
            LoyaltyAccountRepository loyaltyAccountRepository,
            RuleVersionRepository ruleVersionRepository,
            LoyaltyTransactionRepository loyaltyTransactionRepository,
            PointsLotRepository pointsLotRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            AuditEventRepository auditEventRepository,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            PlatformTransactionManager transactionManager
    ) {
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.ruleVersionRepository = ruleVersionRepository;
        this.loyaltyTransactionRepository = loyaltyTransactionRepository;
        this.pointsLotRepository = pointsLotRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        // Built explicitly (instead of @Transactional) so we can catch the
        // unique-constraint violation from a concurrent idempotent request
        // and read back the winning record in a follow-up call - see earn().
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public EarningResponse earn(
            EarningRequest request,
            String idempotencyKey,
            String clientId
    ) {
        String requestJson = serialize(request);
        String requestHash = hash(requestJson);

        EarningResponse existingResponse =
                loadMatchingIdempotentResponse(clientId, idempotencyKey, requestHash);

        if (existingResponse != null) {
            return existingResponse;
        }

        try {
            return transactionTemplate.execute(status ->
                    processEarn(request, idempotencyKey, clientId, requestHash)
            );
        } catch (DataIntegrityViolationException possibleRace) {
            // Only the idempotency unique constraint (uk_client_endpoint_key)
            // represents a concurrent-retry race we know how to resolve.
            // Any other integrity violation (a different constraint, FK
            // violation, etc.) is a real error and must propagate as-is -
            // it must not be silently reinterpreted as an idempotency
            // conflict.
            if (!isIdempotencyUniqueConstraintViolation(possibleRace)) {
                throw possibleRace;
            }

            // Two requests with the same idempotency key both found no
            // existing record and both tried to proceed; the DB unique
            // constraint is the final arbiter and rejected this one. Our
            // own attempt was rolled back - go read back whichever request
            // actually won and resolve accordingly.
            EarningResponse winningResponse =
                    loadMatchingIdempotentResponse(clientId, idempotencyKey, requestHash);

            if (winningResponse == null) {
                // Should not happen: the constraint violation implies a row
                // exists. Surface a conflict rather than silently retrying.
                throw new IdempotencyConflictException(idempotencyKey);
            }

            return winningResponse;
        }
    }

    private static final String IDEMPOTENCY_UNIQUE_CONSTRAINT_NAME = "uk_client_endpoint_key";

    /**
     * True only when the violated constraint is the idempotency table's
     * unique constraint. Prefers the driver-reported constraint name
     * (via Hibernate's ConstraintViolationException) and only falls back
     * to a message-text check when the constraint name isn't available -
     * some JDBC drivers/DBs don't always populate it.
     */
    private boolean isIdempotencyUniqueConstraintViolation(
            DataIntegrityViolationException exception
    ) {
        Throwable mostSpecificCause = exception.getMostSpecificCause();

        if (mostSpecificCause instanceof org.hibernate.exception.ConstraintViolationException hibernateCause) {
            String constraintName = hibernateCause.getConstraintName();
            if (constraintName != null) {
                return constraintName.toLowerCase()
                        .contains(IDEMPOTENCY_UNIQUE_CONSTRAINT_NAME);
            }
        }

        String message = mostSpecificCause != null
                ? mostSpecificCause.getMessage()
                : exception.getMessage();

        return message != null
                && message.toLowerCase().contains(IDEMPOTENCY_UNIQUE_CONSTRAINT_NAME);
    }

    /**
     * Looks up an existing idempotency record for this (clientId, endpoint,
     * idempotencyKey). Returns the previously saved response if the request
     * hash matches (safe retry), throws LOYALTY_IDEMPOTENCY_CONFLICT if it
     * differs, or returns null if no record exists yet.
     */
    private EarningResponse loadMatchingIdempotentResponse(
            String clientId,
            String idempotencyKey,
            String requestHash
    ) {
        return idempotencyRecordRepository
                .findByClientIdAndEndpointAndIdempotencyKey(
                        clientId,
                        ENDPOINT,
                        idempotencyKey
                )
                .map(record -> {
                    if (!record.getRequestHash().equals(requestHash)) {
                        throw new IdempotencyConflictException(idempotencyKey);
                    }
                    return deserialize(record.getResponseBody());
                })
                .orElse(null);
    }

    /**
     * The full earning business transaction: account lock, validation, rule
     * selection, calculation, persistence, audit trail, and the idempotency
     * record write that guards it all. Runs inside a single DB transaction
     * managed by transactionTemplate in earn().
     */
    private EarningResponse processEarn(
            EarningRequest request,
            String idempotencyKey,
            String clientId,
            String requestHash
    ) {
        LoyaltyAccount account = loyaltyAccountRepository
                .findByIdForUpdate(request.getAccountId())
                .orElseThrow(() ->
                        new AccountNotFoundException(request.getAccountId())
                );

        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new AccountFrozenException(account.getId());
        }

        LoyaltyProgram program = account.getProgram();

        boolean transactionExists =
                loyaltyTransactionRepository
                        .existsByAccount_Program_IdAndSourceTransactionIdAndType(
                                program.getId(),
                                request.getSourceTransactionId(),
                                TransactionType.EARN
                        );

        if (transactionExists) {
            throw new DuplicateTransactionException(
                    request.getSourceTransactionId()
            );
        }

        if (program.getStatus() == ProgramStatus.INACTIVE) {
            throw new ProgramInactiveException(program.getId());
        }

        if (!request.getAmount().getCurrency().equals(program.getCurrency())) {
            throw new CurrencyMismatchException(
                    program.getId(),
                    program.getCurrency(),
                    request.getAmount().getCurrency()
            );
        }

        Long programId = program.getId();

        List<RuleVersion> effectiveRules = ruleVersionRepository
                .findEffectiveRules(
                        programId,
                        request.getTransactionTime(),
                        RuleStatus.ACTIVE
                );

        if (effectiveRules.isEmpty()) {
            throw new RuleNotFoundException(
                    programId,
                    request.getTransactionTime()
            );
        }

        if (effectiveRules.size() > 1) {
            throw new MultipleActiveRulesFoundException(
                    programId,
                    request.getTransactionTime()
            );
        }

        RuleVersion rule = effectiveRules.get(0);

        BigDecimal earnedPointsDecimal =
                request.getAmount()
                        .getValue()
                        .multiply(rule.getEarningRate());

        Integer earnedPoints =
                earnedPointsDecimal
                        .setScale(
                                0,
                                mapToJavaRoundingMode(rule.getRoundingMode())
                        )
                        .intValue();

        LoyaltyTransaction transaction =
                LoyaltyTransaction.builder()
                        .account(account)
                        .type(TransactionType.EARN)
                        .sourceTransactionId(
                                request.getSourceTransactionId()
                        )
                        .points(earnedPoints)
                        .moneyAmount(request.getAmount().getValue())
                        .status(TransactionStatus.COMMITTED)
                        .idempotencyKey(idempotencyKey)
                        .build();

        loyaltyTransactionRepository.save(transaction);

        recordCalculationAudit(
                transaction,
                clientId,
                program,
                rule,
                request,
                earnedPoints
        );

        LocalDateTime unlockAt =
                request.getTransactionTime()
                        .plusDays(program.getLockDays());

        LocalDateTime expiresAt =
                request.getTransactionTime()
                        .plusDays(program.getExpiryDays());

        PointsLot pointsLot =
                PointsLot.builder()
                        .account(account)
                        .earningTransaction(transaction)
                        .originalPoints(earnedPoints)
                        .remainingPoints(earnedPoints)
                        .unlockAt(unlockAt)
                        .expiresAt(expiresAt)
                        .status(LotStatus.LOCKED)
                        .build();

        pointsLotRepository.save(pointsLot);

        account.setLockedPoints(
                account.getLockedPoints() + earnedPoints
        );

        loyaltyAccountRepository.save(account);

        BalanceDto balance = new BalanceDto(
                account.getAvailablePoints(),
                account.getLockedPoints(),
                account.getReservedPoints(),
                account.getTotalOwned()
        );

        EarningResponse response = new EarningResponse(
                transaction.getId(),
                request.getSourceTransactionId(),
                earnedPoints,
                pointsLot.getStatus(),
                pointsLot.getUnlockAt(),
                pointsLot.getExpiresAt(),
                rule.getId(),
                balance
        );

        String responseBody = serialize(response);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .clientId(clientId)
                .endpoint(ENDPOINT)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .responseBody(responseBody)
                .build();

        // Final atomic guard: if a concurrent request already inserted a
        // record for this (clientId, endpoint, idempotencyKey), the unique
        // constraint throws here, rolling back everything in this
        // transaction (transaction, points lot, account update, audit
        // event). earn() catches it and reads back the winning record.
        idempotencyRecordRepository.save(record);

        // Published now, delivered only after the transaction commits (see
        // PointsEarnedEventListener) - never call the notification
        // service directly from here.
        eventPublisher.publishEvent(
                new PointsEarnedEvent(
                        account.getId(),
                        transaction.getId(),
                        earnedPoints
                )
        );

        return response;
    }

    private void recordCalculationAudit(
            LoyaltyTransaction transaction,
            String clientId,
            LoyaltyProgram program,
            RuleVersion rule,
            EarningRequest request,
            Integer earnedPoints
    ) {
        AuditEvent.CalculationDetails details = new AuditEvent.CalculationDetails(
                program.getId(),
                rule.getId(),
                request.getAmount().getCurrency(),
                request.getAmount().getValue(),
                earnedPoints
        );

        AuditEvent auditEvent = AuditEvent.builder()
                .actorId(clientId)
                .action("EARN_POINTS_CALCULATED")
                .entityType("LoyaltyTransaction")
                .entityId(transaction.getId())
                .afterJson(serialize(details))
                .correlationId(MDC.get(CorrelationIdFilter.MDC_KEY))
                .build();

        auditEventRepository.save(auditEvent);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize " + value.getClass().getSimpleName(), e);
        }
    }

    private EarningResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, EarningResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize idempotency response", e);
        }
    }

    private String hash(String requestJson) {
        try {
            return bytesToHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(requestJson.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate request hash", e);
        }
    }

    private java.math.RoundingMode mapToJavaRoundingMode(
            com.aman.acceptance.loyalty.enums.RoundingMode mode
    ) {
        return switch (mode) {
            case FLOOR -> java.math.RoundingMode.FLOOR;
        };
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();

        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }
}
