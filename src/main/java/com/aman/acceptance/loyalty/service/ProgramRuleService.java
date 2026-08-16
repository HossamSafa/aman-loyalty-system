package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.enums.RuleStatus;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import com.aman.acceptance.loyalty.model.RuleVersion;
import com.aman.acceptance.loyalty.model.dto.request.ProgramRuleRequest;
import com.aman.acceptance.loyalty.model.dto.response.ProgramRuleResponse;
import com.aman.acceptance.loyalty.repository.LoyaltyProgramRepository;
import com.aman.acceptance.loyalty.repository.RuleVersionRepository;
import com.aman.acceptance.loyalty.service.validators.ProgramRuleValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProgramRuleService {

    private final LoyaltyProgramRepository loyaltyProgramRepository;
    private final RuleVersionRepository ruleVersionRepository;
    private final ProgramRuleValidator programRuleValidator;
    @CacheEvict(
            value = "activeProgramRules",
            key = "#p0"
    )
    @Transactional
    public ProgramRuleResponse updateRules(Long programId, ProgramRuleRequest request) {

        // 1. Find program
        LoyaltyProgram program = loyaltyProgramRepository
                .findById(programId)
                .orElseThrow(() ->
                        BusinessException.notFound(
                                ErrorCode.LOYALTY_PROGRAM_NOT_FOUND,
                                "Loyalty program not found with id: " + programId
                        )
                );

        // 2. Lock and find current rule
        LocalDateTime now = LocalDateTime.now();

        /*RuleVersion currentRule = ruleVersionRepository
                .findEffectiveRuleForUpdate(programId, now)
                .orElseThrow(() ->
                        BusinessException.notFound(
                                ErrorCode.LOYALTY_RULE_NOT_FOUND,
                                "No active rule found for program id: " + programId
                        )
                );*/
        Optional<RuleVersion> currentRuleOptional =
                ruleVersionRepository.findEffectiveRuleForUpdate(
                        programId,
                        now
                );

        RuleVersion currentRule =
                currentRuleOptional.orElse(null);

        if (currentRule != null) {
            // 3. Validate request
            programRuleValidator.validate(
                    programId,
                    request,
                    currentRule
            );
            // 4. Close old rule
            currentRule.setEffectiveTo(
                    request.getEffectiveFrom()
            );

            if (!request.getEffectiveFrom().isAfter(now)) {
                currentRule.setStatus(RuleStatus.CLOSED);
            }

        } else {

            programRuleValidator.validateFirstRule(
                    programId,
                    request
            );
        }


        // 5. Calculate new version number
        int newVersion = ruleVersionRepository
                .findTopByProgramIdOrderByVersionDesc(programId)
                .map(rule -> rule.getVersion() + 1)
                .orElse(1);
        // 6. Create new RuleVersion
        BigDecimal earningRate =
                BigDecimal.valueOf(request.getEarning().getPoints())
                        .divide(request.getEarning().getSpendAmount());

        BigDecimal redemptionRate =
                request.getRedemption().getDiscountAmount()
                        .divide(
                                BigDecimal.valueOf(
                                        request.getRedemption().getPoints()
                                )
                        );
        RuleStatus newStatus =
                request.getEffectiveFrom().isAfter(now)
                        ? RuleStatus.SCHEDULED
                        : RuleStatus.ACTIVE;

        RuleVersion newRule = RuleVersion.builder()
                .program(program)
                .version(newVersion)
                .earningRate(earningRate)
                .redemptionRate(redemptionRate)
                .roundingMode(request.getEarning().getRoundingMode())
                .minimumRedemptionPoints(
                        request.getRedemption().getMinimumPoints()
                )
                .lockDays(request.getLockDays())
                .expiryDays(request.getExpiryDays())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(null)
                .status(newStatus)
                .build();

        // 7. Save new rule
        RuleVersion savedRule = ruleVersionRepository.save(newRule);
        // 8. Return response
        return ProgramRuleResponse.builder()
                .programId(savedRule.getProgram().getId())
                .ruleVersion(savedRule.getVersion())
                .status(savedRule.getStatus())
                .effectiveFrom(savedRule.getEffectiveFrom())
                .lockDays(savedRule.getLockDays())
                .expiryDays(savedRule.getExpiryDays())
                .build();

    }


}
