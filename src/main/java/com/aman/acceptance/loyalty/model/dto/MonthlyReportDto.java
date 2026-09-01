package com.aman.acceptance.loyalty.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportDto {

    private String month;

    private Long pointsIssued;
    private Long pointsRedeemed;
    private Long pointsExpired;
}