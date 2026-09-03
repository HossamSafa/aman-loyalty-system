package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.model.AuditEvent;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.response.AccountSummaryResponse;
import com.aman.acceptance.loyalty.model.response.ActivityFeedItemResponse;
import com.aman.acceptance.loyalty.repository.AuditEventRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountSummaryService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final AuditEventRepository auditEventRepository;

    public AccountSummaryResponse getSummary(Long accountId) {

        LoyaltyAccount account = loyaltyAccountRepository.findById(accountId)
                .orElseThrow(() -> LoyaltyException.notFound(
                        ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                        "No loyalty account exists with id: " + accountId
                ));

        var recentActivity = auditEventRepository.findTop5ByEntityIdOrderByCreatedAtDesc(accountId).stream()
                .map(this::toActivityItem)
                .toList();

        return AccountSummaryResponse.builder()
                .accountId(account.getId())
                .status(account.getStatus().name())
                .availablePoints(account.getAvailablePoints())
                .lockedPoints(account.getLockedPoints())
                .reservedPoints(account.getReservedPoints())
                .recentActivity(recentActivity)
                .build();
    }

    private ActivityFeedItemResponse toActivityItem(AuditEvent event) {
        return ActivityFeedItemResponse.builder()
                .auditId(event.getId())
                .action(event.getAction())
                .accountId(event.getEntityId())
                .actorId(event.getActorId())
                .occurredAt(event.getCreatedAt())
                .build();
    }
}