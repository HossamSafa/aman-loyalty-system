package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.model.dto.response.MonthlyReportResponse;
import com.aman.acceptance.loyalty.model.dto.response.ReportSummaryResponse;
import com.aman.acceptance.loyalty.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final LoyaltyProgramRepository loyaltyProgramRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final RedemptionRepository redemptionRepository;
    private final PointsLotRepository pointsLotRepository;

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary(
            Long programId,
            LocalDateTime from,
            LocalDateTime to
    ) {

        validateReportRequest(programId, from, to);


        Long activeCustomers =
                loyaltyAccountRepository.countActiveCustomers(programId);

        Long newlyEnrolledCustomers =
                loyaltyAccountRepository.countNewCustomers(
                        programId,
                        from,
                        to
                );

        Long pointsIssued =
                loyaltyTransactionRepository.sumPointsByType(
                        programId,
                        TransactionType.EARN,
                        from,
                        to
                );

        Long redeemedPointsRaw =
                loyaltyTransactionRepository.sumPointsByType(
                        programId,
                        TransactionType.REDEEM,
                        from,
                        to
                );

        Long expiredPointsRaw =
                loyaltyTransactionRepository.sumPointsByType(
                        programId,
                        TransactionType.EXPIRE,
                        from,
                        to
                );

        long pointsRedeemed =
                Math.abs(redeemedPointsRaw);

        long pointsExpired =
                Math.abs(expiredPointsRaw);

        BigDecimal redemptionValue =
                redemptionRepository.sumCommittedRedemptionValue(
                        programId,
                        from,
                        to
                );

        Long redemptionCount =
                redemptionRepository.countCommittedRedemptions(
                        programId,
                        from,
                        to
                );
        Long pointsUnlocked =
                pointsLotRepository.sumUnlockedPoints(
                        programId,
                        from,
                        to
                );

        return ReportSummaryResponse.builder()
                .programId(programId)
                .from(from)
                .to(to)
                .activeCustomers(activeCustomers)
                .newlyEnrolledCustomers(newlyEnrolledCustomers)
                .pointsIssued(pointsIssued)
                .pointsUnlocked(pointsUnlocked)
                .pointsRedeemed(pointsRedeemed)
                .pointsExpired(pointsExpired)
                .redemptionValue(redemptionValue)
                .redemptionCount(redemptionCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MonthlyReportResponse> getMonthlyTrend(
            Long programId,
            LocalDateTime from,
            LocalDateTime to
    ) {

        validateReportRequest(programId, from, to);

        List<MonthlyPointsTrendProjection> results =
                loyaltyTransactionRepository.findMonthlyPointsTrend(
                        programId,
                        from,
                        to
                );

        Map<YearMonth, MonthlyPointsTrendProjection> resultByMonth =
                results.stream()
                        .collect(Collectors.toMap(
                                result -> YearMonth.from(result.getMonth()),
                                Function.identity()
                        ));

        YearMonth startMonth = YearMonth.from(from);
        YearMonth endMonth = YearMonth.from(to);

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM yyyy");

        return Stream.iterate(
                        startMonth,
                        month -> !month.isAfter(endMonth),
                        month -> month.plusMonths(1)
                )
                .map(month -> {

                    MonthlyPointsTrendProjection result =
                            resultByMonth.get(month);

                    return MonthlyReportResponse.builder()
                            .month(month.format(formatter))
                            .pointsIssued(
                                    result != null
                                            ? result.getPointsIssued()
                                            : 0L
                            )
                            .pointsRedeemed(
                                    result != null
                                            ? result.getPointsRedeemed()
                                            : 0L
                            )
                            .pointsExpired(
                                    result != null
                                            ? result.getPointsExpired()
                                            : 0L
                            )
                            .build();
                })
                .toList();
    }
    private void validateReportRequest(
            Long programId,
            LocalDateTime from,
            LocalDateTime to
    ) {

        if (!loyaltyProgramRepository.existsById(programId)) {
            throw BusinessException.notFound(
                    ErrorCode.LOYALTY_PROGRAM_NOT_FOUND,
                    "Loyalty program not found: " + programId
            );
        }

        if (from == null || to == null) {
            throw BusinessException.badRequest(
                    ErrorCode.INVALID_REPORT_DATE_RANGE,
                    "From and to dates are required"
            );
        }

        if (from.isAfter(to)) {
            throw BusinessException.badRequest(
                    ErrorCode.INVALID_REPORT_DATE_RANGE,
                    "From date must not be after to date"
            );
        }
    }
}