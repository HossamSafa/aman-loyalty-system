package com.aman.acceptance.loyalty.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryResponse {

    private Long programId;

    private LocalDateTime from;
    private LocalDateTime to;

    private Long activeCustomers;
    private Long newlyEnrolledCustomers;

    private Long pointsIssued;
    private Long pointsUnlocked;
    private Long pointsRedeemed;
    private Long pointsExpired;

    private BigDecimal redemptionValue;
    private Long redemptionCount;
}