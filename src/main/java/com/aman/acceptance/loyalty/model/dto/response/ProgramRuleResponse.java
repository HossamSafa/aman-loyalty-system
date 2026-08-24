package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.enums.RuleStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramRuleResponse {

    private Long programId;

    private Integer ruleVersion;

    private RuleStatus status;

    private LocalDateTime effectiveFrom;

    private Integer lockDays;

    private Integer expiryDays;
}