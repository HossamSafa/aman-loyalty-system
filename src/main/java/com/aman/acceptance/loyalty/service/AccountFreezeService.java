package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.AccountStatus;
import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.*;
import com.aman.acceptance.loyalty.model.AuditEvent;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.request.FreezeAccountRequest;
import com.aman.acceptance.loyalty.model.request.UnfreezeAccountRequest;
import com.aman.acceptance.loyalty.model.response.AccountStatusResponse;
import com.aman.acceptance.loyalty.repository.AuditEventRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class AccountFreezeService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AccountStatusResponse freeze(Long accountId, FreezeAccountRequest request){

        LoyaltyAccount account = loyaltyAccountRepository.findById(accountId)
                .orElseThrow(() -> LoyaltyException.notFound(
                        ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                        "No loyalty account exists with id: " + accountId));

        if (account.getStatus() == AccountStatus.FROZEN) {
            throw LoyaltyException.conflict(
                    ErrorCode.LOYALTY_ACCOUNT_ALREADY_FROZEN,
                    "Account " + accountId + " is already frozen.");
        }


        String beforeJson = buildSnapshotJson(account, null, null);

        // TODO: هنا محتاجين نربط مع Flow 4 (Redemption)
        // المفروض نجيب كل الـ Redemption اللي حالتها OTP_PENDING أو AUTHORIZED
        // وبعدها نلغي الحجز على النقط دي (CANCELLED) ونرجعها Available للحساب
        cancelActiveReservationsMock(accountId);

        account.setStatus(AccountStatus.FROZEN);
        loyaltyAccountRepository.save(account);

        LocalDateTime changedAt = LocalDateTime.now();
        String afterJson = buildSnapshotJson(account, request.getReasonCode(), request.getNote());

        AuditEvent auditEvent = saveAuditEvent(request.getActorId(), "FREEZE", accountId, beforeJson, afterJson);

        log.info("Account [{}] FROZEN by actor [{}] - reason: {}", accountId, request.getActorId(), request.getReasonCode());

        return AccountStatusResponse.builder()
                .accountId(account.getId())
                .status(account.getStatus().name())
                .changedAt(changedAt)
                .auditId(auditEvent.getId())
                .build();
    }

    @Transactional
    public AccountStatusResponse unfreeze(Long accountId, UnfreezeAccountRequest request) {

        LoyaltyAccount account = loyaltyAccountRepository.findById(accountId)
                .orElseThrow(() -> LoyaltyException.notFound(
                        ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                        "No loyalty account exists with id: " + accountId));

        if (account.getStatus() != AccountStatus.FROZEN) {
            throw LoyaltyException.conflict(
                    ErrorCode.LOYALTY_ACCOUNT_NOT_FROZEN,
                    "Account " + accountId + " is not frozen, cannot unfreeze.");
        }

        String beforeJson = buildSnapshotJson(account, null, null);

        account.setStatus(AccountStatus.ACTIVE);
        loyaltyAccountRepository.save(account);

        LocalDateTime changedAt = LocalDateTime.now();
        String afterJson = buildSnapshotJson(account, request.getReasonCode(), request.getNote());

        AuditEvent auditEvent = saveAuditEvent(request.getActorId(), "UNFREEZE", accountId, beforeJson, afterJson);

        log.info("Account [{}] UNFROZEN by actor [{}] - reason: {}", accountId, request.getActorId(), request.getReasonCode());

        return AccountStatusResponse.builder()
                .accountId(account.getId())
                .status(account.getStatus().name())
                .changedAt(changedAt)
                .auditId(auditEvent.getId())
                .build();
    }

    // ================= Helper methods =================

    private void cancelActiveReservationsMock(Long accountId) {
        log.info("MOCK: cancelling active reservations for account [{}] (real logic pending Flow 4)", accountId);
    }

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


    private String buildSnapshotJson(LoyaltyAccount account, String reasonCode, String note) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("accountId", account.getId());
        snapshot.put("status", account.getStatus().name());
        snapshot.put("availablePoints", account.getAvailablePoints());
        snapshot.put("lockedPoints", account.getLockedPoints());
        snapshot.put("reservedPoints", account.getReservedPoints());

        if (reasonCode != null) {
            snapshot.put("reasonCode", reasonCode);
        }
        if (note != null) {
            snapshot.put("note", note);
        }

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.warn("Failed to serialize snapshot for account [{}]", account.getId(), e);
            return "{}";
        }
    }

    public void assertAccountActive(LoyaltyAccount account) {
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw LoyaltyException.locked(
                    ErrorCode.LOYALTY_ACCOUNT_FROZEN,
                    "This loyalty account is temporarily frozen.");
        }
    }

}
