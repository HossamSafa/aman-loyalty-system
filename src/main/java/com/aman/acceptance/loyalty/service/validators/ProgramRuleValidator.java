package com.aman.acceptance.loyalty.service.validators;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.model.RuleVersion;
import com.aman.acceptance.loyalty.model.dto.request.ProgramRuleRequest;
import com.aman.acceptance.loyalty.repository.RuleVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramRuleValidator {
    private final RuleVersionRepository ruleVersionRepository;
    public void validateFirstRule(
            Long programId,
            ProgramRuleRequest request,
            LocalDateTime now
    ) {

        validateEffectiveFromRequired(request,now);
        validateLockDays(request);
        validateExpiryDays(request);
        validateEarningRule(request);
        validateRedemptionRule(request);
        validateNoExistingRules(programId);
    }
    private void validateNoExistingRules(Long programId) {

        List<RuleVersion> rules =
                ruleVersionRepository.findByProgramId(programId);

        if (!rules.isEmpty()) {
            throw BusinessException.conflict(
                    ErrorCode.RULE_EFFECTIVE_PERIOD_CONFLICT,
                    "A rule version already exists for this program"
            );
        }
    }
    private void validateEffectiveFromRequired(
            ProgramRuleRequest request,LocalDateTime now
    ) {

        if (request.getEffectiveFrom() == null) {
            throw BusinessException.badRequest(
                    ErrorCode.INVALID_RULE_EFFECTIVE_DATE,
                    "Effective from date is required"
            );
        }
        // reject effective from if it is in the past
        if (request.getEffectiveFrom().isBefore(now)) {
            throw BusinessException.badRequest(
                    ErrorCode.INVALID_RULE_EFFECTIVE_DATE,
                    "Effective from date cannot be in the past"
            );
        }
    }
    public void validate(
            Long programId,
            ProgramRuleRequest request,
            RuleVersion currentRule,
            LocalDateTime now
    ) {

        validateEffectiveFrom(request, currentRule,now);
        validateLockDays(request);
        validateExpiryDays(request);
        validateEarningRule(request);
        validateRedemptionRule(request);
        validateNoOverlap(programId, currentRule, request);
    }
    private void validateNoOverlap(
            Long programId,
            RuleVersion currentRule,
            ProgramRuleRequest request
    ) {

        LocalDateTime newFrom = request.getEffectiveFrom();

        List<RuleVersion> rules =
                ruleVersionRepository.findByProgramId(programId);

        for (RuleVersion existingRule : rules) {

            if (existingRule.getId().equals(currentRule.getId())) {
                continue;
            }

            LocalDateTime existingTo =
                    existingRule.getEffectiveTo();

            boolean overlaps =
                    existingTo == null
                            || existingTo.isAfter(newFrom);

            if (overlaps) {
                throw BusinessException.conflict(
                        ErrorCode.RULE_EFFECTIVE_PERIOD_CONFLICT,
                        "New rule effective period overlaps with another rule version"
                );
            }
        }
    }
    /*
    private void validateEffectiveFrom(ProgramRuleRequest request, RuleVersion currentRule,LocalDateTime now) {

        if (request.getEffectiveFrom() == null) {
            throw BusinessException.badRequest(
                    ErrorCode.INVALID_RULE_EFFECTIVE_DATE,
                    "Effective from date is required"
            );
        }

        // Reject Effective from if it is in the past
        if (request.getEffectiveFrom().isBefore(now)) {
            throw BusinessException.badRequest(
                    ErrorCode.INVALID_RULE_EFFECTIVE_DATE,
                    "Effective from date cannot be in the past"
            );
        }


        if (!request.getEffectiveFrom()
                .isAfter(currentRule.getEffectiveFrom())) {

            throw BusinessException.conflict(
                    ErrorCode.RULE_EFFECTIVE_PERIOD_CONFLICT,
                    "New rule effective date must be after the current rule effective date"
            );
        }
    }*/
    private void validateEffectiveFrom(
            ProgramRuleRequest request,
            RuleVersion currentRule,
            LocalDateTime now
    ) {

        validateEffectiveFromRequired(request, now);

        if (!request.getEffectiveFrom()
                .isAfter(currentRule.getEffectiveFrom())) {

            throw BusinessException.conflict(
                    ErrorCode.RULE_EFFECTIVE_PERIOD_CONFLICT,
                    "New rule effective date must be after the current rule effective date"
            );
        }
    }

    private void validateLockDays(ProgramRuleRequest request) {

        if (request.getLockDays() == null
                || request.getLockDays() < 0) {

            throw BusinessException.invalid(
                    ErrorCode.INVALID_LOCK_DAYS,
                    "Lock days must be zero or greater"
            );
        }
    }

    private void validateExpiryDays(ProgramRuleRequest request) {

        if (request.getExpiryDays() == null
                || request.getExpiryDays() <= 0) {

            throw BusinessException.invalid(
                    ErrorCode.INVALID_EXPIRY_DAYS,
                    "Expiry days must be greater than zero"
            );
        }
    }

    private void validateEarningRule(ProgramRuleRequest request) {

        if (request.getEarning() == null) {
            throw BusinessException.badRequest(
                    ErrorCode.INVALID_EARNING_RULE,
                    "Earning rule is required"
            );
        }

        if (request.getEarning().getSpendAmount() == null
                || request.getEarning().getSpendAmount().signum() <= 0
                || request.getEarning().getPoints() == null
                || request.getEarning().getPoints() <= 0) {

            throw BusinessException.invalid(
                    ErrorCode.INVALID_EARNING_RULE,
                    "Earning spend amount and points must be greater than zero"
            );
        }
        if (request.getEarning().getRoundingMode() == null) {
            throw BusinessException.invalid(
                    ErrorCode.INVALID_ROUNDING_MODE,
                    "Rounding mode is required"
            );
        }
    }

    private void validateRedemptionRule(ProgramRuleRequest request) {

        if (request.getRedemption() == null) {
            throw BusinessException.badRequest(
                    ErrorCode.INVALID_REDEMPTION_RULE,
                    "Redemption rule is required"
            );
        }

        if (request.getRedemption().getPoints() == null
                || request.getRedemption().getPoints() <= 0
                || request.getRedemption().getDiscountAmount() == null
                || request.getRedemption().getDiscountAmount().signum() <= 0
                || request.getRedemption().getMinimumPoints() == null
                || request.getRedemption().getMinimumPoints() <= 0) {

            throw BusinessException.invalid(
                    ErrorCode.INVALID_REDEMPTION_RULE,
                    "Redemption values must be greater than zero"
            );
        }
    }

}
