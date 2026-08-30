package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.enums.RoundingMode;
import com.aman.acceptance.loyalty.enums.RuleStatus;
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
public class ProgramRuleDetailsResponse {

    private Long id;
    private Long programId;
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