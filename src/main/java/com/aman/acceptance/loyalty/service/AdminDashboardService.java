package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.dto.response.AuditEventResponse;
import com.aman.acceptance.loyalty.model.dto.response.BalanceOverviewResponse;
import com.aman.acceptance.loyalty.model.dto.response.OtpFunnelResponse;
import com.aman.acceptance.loyalty.model.dto.response.PointsFlowResponse;
import com.aman.acceptance.loyalty.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PointsLotRepository pointsLotRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final RedemptionRepository redemptionRepository;
    private final AuditEventRepository auditEventRepository;

    public BalanceOverviewResponse getBalanceOverview() {


        List<Object[]> results = loyaltyAccountRepository.getBalanceTotals();
        Object[] result = results.isEmpty() ? new Object[3] : results.get(0);
        Long available = getLongValue(result, 0);
        Long locked = getLongValue(result, 1);
        Long reserved = getLongValue(result, 2);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusDays(30);

        Long expiringSoon = Optional.ofNullable(
                pointsLotRepository.getExpiringSoonPoints(now, cutoff)
        ).orElse(0L);

        Long totalOwned = available + locked + reserved;

        return new BalanceOverviewResponse(
                available,
                locked,
                reserved,
                expiringSoon,
                totalOwned
        );
    }

    public List<PointsFlowResponse> getPointsFlow() {

        LocalDateTime startDate = LocalDateTime.now().minusMonths(6);

        List<Object[]> results =
                loyaltyTransactionRepository.getPointsFlowByMonth(startDate);

        return results.stream()
                .map(row -> new PointsFlowResponse(
                        (String) row[0],
                        getLongValue(row[1]),
                        getLongValue(row[2])
                ))
                .toList();
    }

    public OtpFunnelResponse getOtpFunnel() {

        LocalDateTime startDate = LocalDateTime.now().minusDays(30);

        List<Object[]> results = redemptionRepository.getOtpFunnelCounts(startDate);
        Object[] result = results.isEmpty() ? new Object[3] : results.get(0);

        Long reserved = getLongValue(result, 0);
        Long verified = getLongValue(result, 1);
        Long committed = getLongValue(result, 2);

        return new OtpFunnelResponse(reserved, verified, committed);
    }

    public List<AuditEventResponse> getRecentAlerts(int limit) {

        List<Object[]> results = auditEventRepository.getRecentAuditEvents(limit);

        return results.stream()
                .map(row -> new AuditEventResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        ((Number) row[4]).longValue(),
                        (String) row[5],
                        ((java.sql.Timestamp) row[6]).toLocalDateTime()
                ))
                .toList();
    }

    private Long getLongValue(Object[] result, int index) {

        if (result == null || result.length <= index || result[index] == null) {
            return 0L;
        }

        return ((Number) result[index]).longValue();
    }

    private Long getLongValue(Object value) {

        if (value == null) {
            return 0L;
        }

        return ((Number) value).longValue();
    }
}