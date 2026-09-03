package com.aman.acceptance.loyalty.model.dto;

import com.aman.acceptance.loyalty.model.dto.response.MoneyResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryDto {

    private Long programId;

    private LocalDateTime from;
    private LocalDateTime to;

    private Long activeCustomers;
    private Long newlyEnrolledCustomers;

    private Long pointsIssued;
    private Long pointsUnlocked;
    private Long pointsRedeemed;
    private Long pointsExpired;

    private MoneyResponseDto redemptionValue;
    private Long redemptionCount;
    private List<MonthlyReportDto> monthlyTrend;
}