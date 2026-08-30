package com.aman.acceptance.loyalty.service.validators;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.enums.RoundingMode;
import com.aman.acceptance.loyalty.enums.RuleStatus;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import com.aman.acceptance.loyalty.model.RuleVersion;
import com.aman.acceptance.loyalty.model.dto.request.EarningDto;
import com.aman.acceptance.loyalty.model.dto.request.ProgramRuleRequest;
import com.aman.acceptance.loyalty.model.dto.request.RedemptionDto;
import com.aman.acceptance.loyalty.repository.RuleVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramRuleValidatorTest {

    private static final Long PROGRAM_ID = 1001L;

    @Mock
    private RuleVersionRepository ruleVersionRepository;

    private ProgramRuleValidator validator;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        validator =
                new ProgramRuleValidator(
                        ruleVersionRepository
                );

        now = LocalDateTime.of(
                2026,
                8,
                18,
                12,
                0
        );
    }

    private ProgramRuleRequest validRequest() {

        EarningDto earning =
                new EarningDto();

        earning.setSpendAmount(
                new BigDecimal("100.00")
        );

        earning.setPoints(10);

        earning.setRoundingMode(
                RoundingMode.HALF_UP
        );

        RedemptionDto redemption =
                new RedemptionDto();

        redemption.setPoints(100);

        redemption.setDiscountAmount(
                new BigDecimal("5.00")
        );

        redemption.setMinimumPoints(100);

        ProgramRuleRequest request =
                new ProgramRuleRequest();

        request.setEffectiveFrom(
                now.plusDays(1)
        );

        request.setEarning(
                earning
        );

        request.setRedemption(
                redemption
        );

        request.setLockDays(7);
        request.setExpiryDays(365);

        return request;
    }

    private RuleVersion currentRule() {

        LoyaltyProgram program =
                LoyaltyProgram.builder()
                        .id(PROGRAM_ID)
                        .build();

        return RuleVersion.builder()
                .id(50L)
                .program(program)
                .version(4)
                .effectiveFrom(
                        now.minusDays(30)
                )
                .effectiveTo(null)
                .status(RuleStatus.ACTIVE)
                .build();
    }

    @Test
    void validateFirstRule_whenRequestValid_shouldPass() {

        ProgramRuleRequest request =
                validRequest();

        when(ruleVersionRepository.findByProgramId(PROGRAM_ID))
                .thenReturn(List.of());

        assertThatCode(
                () -> validator.validateFirstRule(
                        PROGRAM_ID,
                        request,
                        now
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_whenRequestValid_shouldPass() {

        ProgramRuleRequest request =
                validRequest();

        RuleVersion currentRule =
                currentRule();

        when(ruleVersionRepository.findByProgramId(PROGRAM_ID))
                .thenReturn(
                        List.of(currentRule)
                );

        assertThatCode(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule,
                        now
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_whenEffectiveFromNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.setEffectiveFrom(null);

        RuleVersion currentRule =
                currentRule();

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule,
                        now
                ),
                ErrorCode.INVALID_RULE_EFFECTIVE_DATE,
                400
        );
    }

    @Test
    void validateFirstRule_whenEffectiveFromNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.setEffectiveFrom(null);

        assertBusinessException(
                () -> validator.validateFirstRule(
                        PROGRAM_ID,
                        request,
                        now
                ),
                ErrorCode.INVALID_RULE_EFFECTIVE_DATE,
                400
        );
    }

    @Test
    void validate_whenEffectiveFromInPast_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.setEffectiveFrom(
                now.minusSeconds(1)
        );

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_RULE_EFFECTIVE_DATE,
                400
        );
    }

    @Test
    void validate_whenEffectiveFromEqualsNow_shouldPassDateValidation() {

        ProgramRuleRequest request =
                validRequest();

        request.setEffectiveFrom(now);

        RuleVersion currentRule =
                currentRule();

        when(ruleVersionRepository.findByProgramId(PROGRAM_ID))
                .thenReturn(
                        List.of(currentRule)
                );

        assertThatCode(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule,
                        now
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_whenEffectiveFromNotAfterCurrentRuleStart_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        RuleVersion currentRule =
                currentRule();

        request.setEffectiveFrom(
                currentRule.getEffectiveFrom()
        );

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule,
                        now.minusDays(40)
                ),
                ErrorCode.RULE_EFFECTIVE_PERIOD_CONFLICT,
                409
        );
    }

    @Test
    void validate_whenLockDaysNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.setLockDays(null);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_LOCK_DAYS,
                422
        );
    }

    @Test
    void validate_whenLockDaysNegative_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.setLockDays(-1);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_LOCK_DAYS,
                422
        );
    }

    @Test
    void validate_whenLockDaysZero_shouldPass() {

        ProgramRuleRequest request =
                validRequest();

        request.setLockDays(0);

        RuleVersion currentRule =
                currentRule();

        when(ruleVersionRepository.findByProgramId(PROGRAM_ID))
                .thenReturn(
                        List.of(currentRule)
                );

        assertThatCode(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule,
                        now
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_whenExpiryDaysNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.setExpiryDays(null);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_EXPIRY_DAYS,
                422
        );
    }

    @Test
    void validate_whenExpiryDaysZero_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.setExpiryDays(0);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_EXPIRY_DAYS,
                422
        );
    }

    @Test
    void validate_whenExpiryDaysNegative_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.setExpiryDays(-1);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_EXPIRY_DAYS,
                422
        );
    }

    @Test
    void validate_whenEarningNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.setEarning(null);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_EARNING_RULE,
                400
        );
    }

    @Test
    void validate_whenSpendAmountNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getEarning()
                .setSpendAmount(null);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_EARNING_RULE,
                422
        );
    }

    @Test
    void validate_whenSpendAmountZero_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getEarning()
                .setSpendAmount(
                        BigDecimal.ZERO
                );

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_EARNING_RULE,
                422
        );
    }

    @Test
    void validate_whenSpendAmountNegative_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getEarning()
                .setSpendAmount(
                        new BigDecimal("-1")
                );

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_EARNING_RULE,
                422
        );
    }

    @Test
    void validate_whenEarningPointsNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getEarning()
                .setPoints(null);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_EARNING_RULE,
                422
        );
    }

    @Test
    void validate_whenEarningPointsZero_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getEarning()
                .setPoints(0);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_EARNING_RULE,
                422
        );
    }

    @Test
    void validate_whenEarningPointsNegative_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getEarning()
                .setPoints(-1);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_EARNING_RULE,
                422
        );
    }

    @Test
    void validate_whenRoundingModeNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getEarning()
                .setRoundingMode(null);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_ROUNDING_MODE,
                422
        );
    }

    @Test
    void validate_whenRedemptionNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.setRedemption(null);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_REDEMPTION_RULE,
                400
        );
    }

    @Test
    void validate_whenRedemptionPointsNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getRedemption()
                .setPoints(null);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_REDEMPTION_RULE,
                422
        );
    }

    @Test
    void validate_whenRedemptionPointsZero_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getRedemption()
                .setPoints(0);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_REDEMPTION_RULE,
                422
        );
    }

    @Test
    void validate_whenRedemptionPointsNegative_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getRedemption()
                .setPoints(-1);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_REDEMPTION_RULE,
                422
        );
    }

    @Test
    void validate_whenDiscountAmountNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getRedemption()
                .setDiscountAmount(null);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_REDEMPTION_RULE,
                422
        );
    }

    @Test
    void validate_whenDiscountAmountZero_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getRedemption()
                .setDiscountAmount(
                        BigDecimal.ZERO
                );

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_REDEMPTION_RULE,
                422
        );
    }

    @Test
    void validate_whenDiscountAmountNegative_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getRedemption()
                .setDiscountAmount(
                        new BigDecimal("-1")
                );

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_REDEMPTION_RULE,
                422
        );
    }

    @Test
    void validate_whenMinimumPointsNull_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getRedemption()
                .setMinimumPoints(null);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_REDEMPTION_RULE,
                422
        );
    }

    @Test
    void validate_whenMinimumPointsZero_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getRedemption()
                .setMinimumPoints(0);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_REDEMPTION_RULE,
                422
        );
    }

    @Test
    void validate_whenMinimumPointsNegative_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        request.getRedemption()
                .setMinimumPoints(-1);

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule(),
                        now
                ),
                ErrorCode.INVALID_REDEMPTION_RULE,
                422
        );
    }

    @Test
    void validateFirstRule_whenAnotherRuleAlreadyExists_shouldReject() {

        ProgramRuleRequest request =
                validRequest();

        RuleVersion existing =
                currentRule();

        when(ruleVersionRepository.findByProgramId(PROGRAM_ID))
                .thenReturn(
                        List.of(existing)
                );

        assertBusinessException(
                () -> validator.validateFirstRule(
                        PROGRAM_ID,
                        request,
                        now
                ),
                ErrorCode.RULE_EFFECTIVE_PERIOD_CONFLICT,
                409
        );
    }

    @Test
    void validate_whenAnotherRuleHasNoEffectiveTo_shouldDetectOverlap() {

        ProgramRuleRequest request =
                validRequest();

        RuleVersion currentRule =
                currentRule();

        RuleVersion otherRule =
                RuleVersion.builder()
                        .id(60L)
                        .effectiveFrom(
                                now.minusDays(5)
                        )
                        .effectiveTo(null)
                        .build();

        when(ruleVersionRepository.findByProgramId(PROGRAM_ID))
                .thenReturn(
                        List.of(
                                currentRule,
                                otherRule
                        )
                );

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule,
                        now
                ),
                ErrorCode.RULE_EFFECTIVE_PERIOD_CONFLICT,
                409
        );
    }

    @Test
    void validate_whenAnotherRuleEndsAfterNewStart_shouldDetectOverlap() {

        ProgramRuleRequest request =
                validRequest();

        RuleVersion currentRule =
                currentRule();

        RuleVersion otherRule =
                RuleVersion.builder()
                        .id(60L)
                        .effectiveFrom(
                                now.minusDays(10)
                        )
                        .effectiveTo(
                                request.getEffectiveFrom()
                                        .plusDays(1)
                        )
                        .build();

        when(ruleVersionRepository.findByProgramId(PROGRAM_ID))
                .thenReturn(
                        List.of(
                                currentRule,
                                otherRule
                        )
                );

        assertBusinessException(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule,
                        now
                ),
                ErrorCode.RULE_EFFECTIVE_PERIOD_CONFLICT,
                409
        );
    }

    @Test
    void validate_whenAnotherRuleEndsExactlyAtNewStart_shouldNotOverlap() {

        ProgramRuleRequest request =
                validRequest();

        RuleVersion currentRule =
                currentRule();

        RuleVersion otherRule =
                RuleVersion.builder()
                        .id(60L)
                        .effectiveFrom(
                                now.minusDays(10)
                        )
                        .effectiveTo(
                                request.getEffectiveFrom()
                        )
                        .build();

        when(ruleVersionRepository.findByProgramId(PROGRAM_ID))
                .thenReturn(
                        List.of(
                                currentRule,
                                otherRule
                        )
                );

        assertThatCode(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule,
                        now
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldIgnoreCurrentRuleDuringOverlapCheck() {

        ProgramRuleRequest request =
                validRequest();

        RuleVersion currentRule =
                currentRule();

        when(ruleVersionRepository.findByProgramId(PROGRAM_ID))
                .thenReturn(
                        List.of(currentRule)
                );

        assertThatCode(
                () -> validator.validate(
                        PROGRAM_ID,
                        request,
                        currentRule,
                        now
                )
        ).doesNotThrowAnyException();
    }

    private void assertBusinessException(
            Runnable action,
            ErrorCode expectedCode,
            int expectedStatus
    ) {

        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(throwable -> {

                    BusinessException exception =
                            (BusinessException) throwable;

                    assertThat(exception.getCode())
                            .isEqualTo(expectedCode);

                    assertThat(
                            exception.getStatus().value()
                    ).isEqualTo(expectedStatus);
                });
    }
}