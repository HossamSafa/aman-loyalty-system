package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.model.response.RiskSummaryResponse;
import com.aman.acceptance.loyalty.repository.AuditEventRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskSummaryService {

    private final AuditEventRepository auditEventRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    public RiskSummaryResponse getSummary() {

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        long frozenCount = auditEventRepository.countDistinctAccountsByActionAndCreatedAtAfter("FREEZE", thirtyDaysAgo);

        long adjustmentsCount = loyaltyTransactionRepository.countByTypeInAndCreatedAtAfter(
                List.of(TransactionType.ADJUSTMENT_CREDIT, TransactionType.ADJUSTMENT_DEBIT),
                thirtyDaysAgo
        );

        return RiskSummaryResponse.builder()
                .accountsFrozenLast30Days(frozenCount)
                .adjustmentsLast30Days(adjustmentsCount)
                .build();
    }
}