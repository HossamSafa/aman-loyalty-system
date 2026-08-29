package com.aman.acceptance.loyalty.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyAccountResponseDto {
    private Long accountId;
    private String customerMobile;
    private String programName;
    private Integer availablePoints;
    private Integer lockedPoints;
    private Integer reservedPoints;
    private String status;
    private LocalDate enrolledDate;
}
