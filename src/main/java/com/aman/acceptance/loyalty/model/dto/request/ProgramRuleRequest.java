package com.aman.acceptance.loyalty.model.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProgramRuleRequest {

    private LocalDateTime effectiveFrom;

    private EarningDto earning;

    private RedemptionDto redemption;

    private Integer lockDays;

    private Integer expiryDays;
}
