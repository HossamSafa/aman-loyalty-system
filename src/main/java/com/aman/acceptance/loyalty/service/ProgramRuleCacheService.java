package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.model.RuleVersion;
import com.aman.acceptance.loyalty.repository.RuleVersionRepository;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class ProgramRuleCacheService {
     private final RuleVersionRepository ruleVersionRepository;

    @Cacheable(
            value = "activeProgramRules",
            key = "#programId"
    )
    public RuleVersion getActiveRule(Long programId) {

        LocalDateTime now = LocalDateTime.now();

        return ruleVersionRepository
                .findEffectiveRuleForUpdate(programId, now)
                .orElseThrow(() ->
                        BusinessException.notFound(
                                ErrorCode.LOYALTY_RULE_NOT_FOUND,
                                "No active rule found for program id: " + programId
                        )
                );
    }
}
