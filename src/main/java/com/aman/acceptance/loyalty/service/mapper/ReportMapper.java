package com.aman.acceptance.loyalty.service.mapper;

import com.aman.acceptance.loyalty.model.dto.MonthlyReportDto;
import com.aman.acceptance.loyalty.repository.MonthlyPointsTrendProjection;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import static com.aman.acceptance.loyalty.util.ReportUtils.valueOrZero;

public final class ReportMapper {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMM yyyy");

    private ReportMapper() {
    }

    public static MonthlyReportDto toMonthlyReport(
            YearMonth month,
            MonthlyPointsTrendProjection result
    ) {
        return MonthlyReportDto.builder()
                .month(month.format(MONTH_FORMATTER))
                .pointsIssued(valueOrZero(result, MonthlyPointsTrendProjection::getPointsIssued))
                .pointsRedeemed(valueOrZero(result, MonthlyPointsTrendProjection::getPointsRedeemed))
                .pointsExpired(valueOrZero(result, MonthlyPointsTrendProjection::getPointsExpired))
                .build();
    }
}