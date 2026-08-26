package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.dto.response.AuditEventResponse;
import com.aman.acceptance.loyalty.model.dto.response.BalanceOverviewResponse;
import com.aman.acceptance.loyalty.model.dto.response.OtpFunnelResponse;
import com.aman.acceptance.loyalty.model.dto.response.PointsFlowResponse;
import com.aman.acceptance.loyalty.repository.AuditEventRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.repository.RedemptionRepository;
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


    // =========================================================
    // BALANCE COMPOSITION
    // =========================================================

    public BalanceOverviewResponse getBalanceOverview() {

        List<Object[]> results = loyaltyAccountRepository.getBalanceTotals();

        Object[] result = results.isEmpty()
                ? new Object[3]
                : results.get(0);

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


    // =========================================================
    // POINTS FLOW
    // =========================================================

    public List<PointsFlowResponse> getPointsFlow() {

        LocalDateTime startDate =
                LocalDateTime.now().minusMonths(6);

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


    // =========================================================
    // OTP FUNNEL
    // =========================================================

    public OtpFunnelResponse getOtpFunnel() {

        LocalDateTime startDate =
                LocalDateTime.now().minusDays(30);

        List<Object[]> results =
                redemptionRepository.getOtpFunnelCounts(startDate);

        Object[] result = results.isEmpty()
                ? new Object[3]
                : results.get(0);

        Long reserved = getLongValue(result, 0);
        Long verified = getLongValue(result, 1);
        Long committed = getLongValue(result, 2);

        return new OtpFunnelResponse(
                reserved,
                verified,
                committed
        );
    }


    // =========================================================
    // RECENT ALERTS
    // =========================================================

    public List<AuditEventResponse> getRecentAlerts(int limit) {

        List<Object[]> results =
                auditEventRepository.getRecentAuditEvents(limit);

        return results.stream()
                .map(row -> new AuditEventResponse(

                        // id
                        row[0] == null
                                ? null
                                : ((Number) row[0]).longValue(),

                        // actorId
                        (String) row[1],

                        // action
                        (String) row[2],

                        // entityType
                        (String) row[3],

                        // entityId
                        row[4] == null
                                ? null
                                : ((Number) row[4]).longValue(),

                        // afterJson
                        (String) row[5],

                        // createdAt
                        (LocalDateTime) row[6]
                ))
                .toList();
    }


    // =========================================================
    // HELPER METHODS
    // =========================================================

    private Long getLongValue(Object[] result, int index) {

        if (result == null
                || result.length <= index
                || result[index] == null) {

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