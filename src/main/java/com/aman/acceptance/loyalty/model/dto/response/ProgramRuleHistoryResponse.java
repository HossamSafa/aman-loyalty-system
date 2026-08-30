package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.enums.RoundingMode;
import com.aman.acceptance.loyalty.enums.RuleStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProgramRuleHistoryResponse {

    private Long id;

    private Long programId;
    private String programName;

    private Integer version;

    private BigDecimal earningRate;
    private BigDecimal redemptionRate;

    private RoundingMode roundingMode;

    private Integer minimumRedemptionPoints;

    private Integer lockDays;
    private Integer expiryDays;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    private RuleStatus status;
}