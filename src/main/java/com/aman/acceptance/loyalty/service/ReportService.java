package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.CurrencyCode;
import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.model.dto.MonthlyReportDto;
import com.aman.acceptance.loyalty.model.dto.response.MoneyResponseDto;
import com.aman.acceptance.loyalty.model.dto.ReportSummaryDto;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.MonthlyPointsTrendProjection;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.repository.RedemptionRepository;
import com.aman.acceptance.loyalty.service.mapper.ReportMapper;
import com.aman.acceptance.loyalty.service.validators.ReportValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final RedemptionRepository redemptionRepository;
    private final PointsLotRepository pointsLotRepository;
    private final ReportValidator reportValidator;

    @Transactional(readOnly = true)
    public ReportSummaryDto getSummary(Long programId, LocalDateTime from, LocalDateTime to) {
        reportValidator.validate(programId, from, to);

        Long activeCustomers = loyaltyAccountRepository.countActiveCustomers(programId);
        Long newlyEnrolledCustomers = loyaltyAccountRepository.countNewCustomers(programId, from, to);
        Long pointsIssued = loyaltyTransactionRepository.sumPointsByType(programId, TransactionType.EARN, from, to);
        Long redeemedPointsRaw = loyaltyTransactionRepository.sumPointsByType(programId, TransactionType.REDEEM, from, to);
        Long expiredPointsRaw = loyaltyTransactionRepository.sumPointsByType(programId, TransactionType.EXPIRE, from, to);

        long pointsRedeemed = Math.abs(redeemedPointsRaw);
        long pointsExpired = Math.abs(expiredPointsRaw);

        BigDecimal redemptionValueAmount =
                redemptionRepository.sumCommittedRedemptionValue(programId, from, to);

        MoneyResponseDto redemptionValue = MoneyResponseDto.builder()
                .value(redemptionValueAmount)
                .currency(CurrencyCode.EGP)
                .build();

        Long redemptionCount = redemptionRepository.countCommittedRedemptions(programId, from, to);
        Long pointsUnlocked = pointsLotRepository.sumUnlockedPoints(programId, from, to);
        List<MonthlyReportDto> monthlyTrend = getMonthlyTrend(programId, from, to);

        return ReportSummaryDto.builder()
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
                .monthlyTrend(monthlyTrend)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MonthlyReportDto> getMonthlyTrend(Long programId, LocalDateTime from, LocalDateTime to) {
        reportValidator.validate(programId, from, to);

        List<MonthlyPointsTrendProjection> results =
                loyaltyTransactionRepository.findMonthlyPointsTrend(programId, from, to);

        Map<YearMonth, MonthlyPointsTrendProjection> resultByMonth =
                results.stream()
                        .collect(Collectors.toMap(
                                result -> YearMonth.from(result.getMonth()),
                                Function.identity()
                        ));

        YearMonth startMonth = YearMonth.from(from);
        YearMonth endMonth = YearMonth.from(to);

        return Stream.iterate(
                        startMonth,
                        month -> !month.isAfter(endMonth),
                        month -> month.plusMonths(1)
                )
                .map(month -> ReportMapper.toMonthlyReport(
                        month,
                        resultByMonth.get(month)
                ))
                .toList();
    }
}