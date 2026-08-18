package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.enums.RoundingMode;
import com.aman.acceptance.loyalty.enums.RuleStatus;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import com.aman.acceptance.loyalty.model.RuleVersion;
import com.aman.acceptance.loyalty.model.dto.request.EarningDto;
import com.aman.acceptance.loyalty.model.dto.request.ProgramRuleRequest;
import com.aman.acceptance.loyalty.model.dto.request.RedemptionDto;
import com.aman.acceptance.loyalty.model.dto.response.ProgramRuleResponse;
import com.aman.acceptance.loyalty.repository.LoyaltyProgramRepository;
import com.aman.acceptance.loyalty.repository.RuleVersionRepository;
import com.aman.acceptance.loyalty.service.validators.ProgramRuleValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramRuleServiceTest {

    private static final Long PROGRAM_ID = 1001L;

    //Don't interact with DB
    @Mock
    private LoyaltyProgramRepository loyaltyProgramRepository;

    @Mock
    private RuleVersionRepository ruleVersionRepository;

    @Mock
    private ProgramRuleValidator programRuleValidator;

    private ProgramRuleService programRuleService;

    // run before each test
    @BeforeEach
    void setUp() {
        programRuleService = new ProgramRuleService(
                loyaltyProgramRepository,
                ruleVersionRepository,
                programRuleValidator
        );
    }

    private LoyaltyProgram buildProgram() {
        return LoyaltyProgram.builder()
                .id(PROGRAM_ID)
                .merchantId("mer-01")
                .name("Aman Rewards")
                .currency("EGP")
                .lockDays(30)
                .expiryDays(360)
                .build();
    }

    private ProgramRuleRequest buildRequest(LocalDateTime effectiveFrom) {

        EarningDto earning = new EarningDto();
        earning.setSpendAmount(new BigDecimal("3.00"));
        earning.setPoints(10);
        earning.setRoundingMode(RoundingMode.HALF_UP);

        RedemptionDto redemption = new RedemptionDto();
        redemption.setPoints(3);
        redemption.setDiscountAmount(new BigDecimal("1.00"));
        redemption.setMinimumPoints(100);

        ProgramRuleRequest request = new ProgramRuleRequest();
        request.setEffectiveFrom(effectiveFrom);
        request.setEarning(earning);
        request.setRedemption(redemption);
        request.setLockDays(7);
        request.setExpiryDays(365);

        return request;
    }

    private RuleVersion buildCurrentRule(
            LoyaltyProgram program,
            int version
    ) {
        return RuleVersion.builder()
                .id(50L)
                .program(program)
                .version(version)
                .earningRate(new BigDecimal("1.0000"))
                .redemptionRate(new BigDecimal("0.5000"))
                .roundingMode(RoundingMode.FLOOR)
                .minimumRedemptionPoints(100)
                .lockDays(7)
                .expiryDays(365)
                .effectiveFrom(LocalDateTime.now().minusDays(30))
                .effectiveTo(null)
                .status(RuleStatus.ACTIVE)
                .build();
    }

    @Test
    void updateRules_whenProgramDoesNotExist_shouldThrowNotFound() {

        ProgramRuleRequest request =
                buildRequest(LocalDateTime.now().plusDays(1));

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> programRuleService.updateRules(
                        PROGRAM_ID,
                        request
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(throwable -> {

                    BusinessException exception =
                            (BusinessException) throwable;

                    assertThat(exception.getCode())
                            .isEqualTo(
                                    ErrorCode.LOYALTY_PROGRAM_NOT_FOUND
                            );

                    assertThat(exception.getStatus().value())
                            .isEqualTo(404);
                });

        verifyNoInteractions(ruleVersionRepository);
        verifyNoInteractions(programRuleValidator);
    }

    @Test
    void updateRules_whenFirstRule_shouldCallFirstRuleValidator() {

        LoyaltyProgram program = buildProgram();

        ProgramRuleRequest request =
                buildRequest(LocalDateTime.now().plusDays(1));

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        when(ruleVersionRepository
                .findTopByProgramIdOrderByVersionDesc(PROGRAM_ID))
                .thenReturn(Optional.empty());

        when(ruleVersionRepository.save(any(RuleVersion.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        programRuleService.updateRules(
                PROGRAM_ID,
                request
        );

        verify(programRuleValidator)
                .validateFirstRule(
                        eq(PROGRAM_ID),
                        eq(request),
                        any(LocalDateTime.class)
                );

        verify(programRuleValidator, never())
                .validate(
                        anyLong(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void updateRules_whenCurrentRuleExists_shouldCallExistingRuleValidator() {

        LoyaltyProgram program = buildProgram();

        RuleVersion currentRule =
                buildCurrentRule(program, 4);

        ProgramRuleRequest request =
                buildRequest(LocalDateTime.now().plusDays(5));

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(currentRule));

        when(ruleVersionRepository
                .findTopByProgramIdOrderByVersionDesc(PROGRAM_ID))
                .thenReturn(Optional.of(currentRule));

        when(ruleVersionRepository.save(any(RuleVersion.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        programRuleService.updateRules(
                PROGRAM_ID,
                request
        );

        verify(programRuleValidator)
                .validate(
                        eq(PROGRAM_ID),
                        eq(request),
                        eq(currentRule),
                        any(LocalDateTime.class)
                );

        verify(programRuleValidator, never())
                .validateFirstRule(
                        anyLong(),
                        any(),
                        any()
                );
    }

    @Test
    void updateRules_whenFirstRule_shouldCreateVersionOne() {

        LoyaltyProgram program = buildProgram();

        ProgramRuleRequest request =
                buildRequest(LocalDateTime.now().plusDays(1));

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        when(ruleVersionRepository
                .findTopByProgramIdOrderByVersionDesc(PROGRAM_ID))
                .thenReturn(Optional.empty());

        when(ruleVersionRepository.save(any(RuleVersion.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        ProgramRuleResponse response =
                programRuleService.updateRules(
                        PROGRAM_ID,
                        request
                );

        assertThat(response.getRuleVersion())
                .isEqualTo(1);

        assertThat(response.getStatus())
                .isEqualTo(RuleStatus.SCHEDULED);
    }

    @Test
    void updateRules_whenLatestVersionIsSeven_shouldCreateVersionEight() {

        LoyaltyProgram program = buildProgram();

        RuleVersion currentRule =
                buildCurrentRule(program, 7);

        ProgramRuleRequest request =
                buildRequest(LocalDateTime.now().plusDays(5));

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(currentRule));

        when(ruleVersionRepository
                .findTopByProgramIdOrderByVersionDesc(PROGRAM_ID))
                .thenReturn(Optional.of(currentRule));

        when(ruleVersionRepository.save(any(RuleVersion.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        ProgramRuleResponse response =
                programRuleService.updateRules(
                        PROGRAM_ID,
                        request
                );

        assertThat(response.getRuleVersion())
                .isEqualTo(8);
    }

    @Test
    void updateRules_whenFutureRuleCreated_shouldCloseCurrentEffectivePeriod() {

        LoyaltyProgram program = buildProgram();

        RuleVersion currentRule =
                buildCurrentRule(program, 4);

        LocalDateTime newEffectiveFrom =
                LocalDateTime.now().plusDays(5);

        ProgramRuleRequest request =
                buildRequest(newEffectiveFrom);

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(currentRule));

        when(ruleVersionRepository
                .findTopByProgramIdOrderByVersionDesc(PROGRAM_ID))
                .thenReturn(Optional.of(currentRule));

        when(ruleVersionRepository.save(any(RuleVersion.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        programRuleService.updateRules(
                PROGRAM_ID,
                request
        );

        assertThat(currentRule.getEffectiveTo())
                .isEqualTo(newEffectiveFrom);

        assertThat(currentRule.getStatus())
                .isEqualTo(RuleStatus.ACTIVE);
    }

    @Test
    void updateRules_whenEffectiveFromIsFuture_shouldCreateScheduledRule() {

        LoyaltyProgram program = buildProgram();

        ProgramRuleRequest request =
                buildRequest(LocalDateTime.now().plusDays(1));

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        when(ruleVersionRepository
                .findTopByProgramIdOrderByVersionDesc(PROGRAM_ID))
                .thenReturn(Optional.empty());

        when(ruleVersionRepository.save(any(RuleVersion.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        ProgramRuleResponse response =
                programRuleService.updateRules(
                        PROGRAM_ID,
                        request
                );

        assertThat(response.getStatus())
                .isEqualTo(RuleStatus.SCHEDULED);
    }

    @Test
    void updateRules_shouldCalculateEarningRateWithScaleAndHalfUpRounding() {

        LoyaltyProgram program = buildProgram();

        ProgramRuleRequest request =
                buildRequest(LocalDateTime.now().plusDays(1));

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        when(ruleVersionRepository
                .findTopByProgramIdOrderByVersionDesc(PROGRAM_ID))
                .thenReturn(Optional.empty());

        when(ruleVersionRepository.save(any(RuleVersion.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        programRuleService.updateRules(
                PROGRAM_ID,
                request
        );

        ArgumentCaptor<RuleVersion> captor =
                ArgumentCaptor.forClass(RuleVersion.class);

        verify(ruleVersionRepository)
                .save(captor.capture());

        RuleVersion savedRule =
                captor.getValue();

        /*
         * 10 / 3 = 3.333333...
         * scale = 4
         * HALF_UP => 3.3333
         */
        assertThat(savedRule.getEarningRate())
                .isEqualByComparingTo("3.3333");
    }

    @Test
    void updateRules_shouldCalculateRedemptionRateWithScaleAndHalfUpRounding() {

        LoyaltyProgram program = buildProgram();

        ProgramRuleRequest request =
                buildRequest(LocalDateTime.now().plusDays(1));

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        when(ruleVersionRepository
                .findTopByProgramIdOrderByVersionDesc(PROGRAM_ID))
                .thenReturn(Optional.empty());

        when(ruleVersionRepository.save(any(RuleVersion.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        programRuleService.updateRules(
                PROGRAM_ID,
                request
        );

        ArgumentCaptor<RuleVersion> captor =
                ArgumentCaptor.forClass(RuleVersion.class);

        verify(ruleVersionRepository)
                .save(captor.capture());

        RuleVersion savedRule =
                captor.getValue();

        /*
         * 1 / 3 = 0.333333...
         * scale = 4
         * HALF_UP => 0.3333
         */
        assertThat(savedRule.getRedemptionRate())
                .isEqualByComparingTo("0.3333");
    }

    @Test
    void updateRules_shouldCopyAllRequestFieldsIntoNewRule() {

        LoyaltyProgram program = buildProgram();

        LocalDateTime effectiveFrom =
                LocalDateTime.now().plusDays(3);

        ProgramRuleRequest request =
                buildRequest(effectiveFrom);

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        when(ruleVersionRepository
                .findTopByProgramIdOrderByVersionDesc(PROGRAM_ID))
                .thenReturn(Optional.empty());

        when(ruleVersionRepository.save(any(RuleVersion.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        programRuleService.updateRules(
                PROGRAM_ID,
                request
        );

        ArgumentCaptor<RuleVersion> captor =
                ArgumentCaptor.forClass(RuleVersion.class);

        verify(ruleVersionRepository)
                .save(captor.capture());

        RuleVersion savedRule =
                captor.getValue();

        assertThat(savedRule.getProgram())
                .isSameAs(program);

        assertThat(savedRule.getVersion())
                .isEqualTo(1);

        assertThat(savedRule.getRoundingMode())
                .isEqualTo(RoundingMode.HALF_UP);

        assertThat(savedRule.getMinimumRedemptionPoints())
                .isEqualTo(100);

        assertThat(savedRule.getLockDays())
                .isEqualTo(7);

        assertThat(savedRule.getExpiryDays())
                .isEqualTo(365);

        assertThat(savedRule.getEffectiveFrom())
                .isEqualTo(effectiveFrom);

        assertThat(savedRule.getEffectiveTo())
                .isNull();

        assertThat(savedRule.getStatus())
                .isEqualTo(RuleStatus.SCHEDULED);
    }

    @Test
    void updateRules_shouldReturnResponseFromSavedRule() {

        LoyaltyProgram program = buildProgram();

        LocalDateTime effectiveFrom =
                LocalDateTime.now().plusDays(2);

        ProgramRuleRequest request =
                buildRequest(effectiveFrom);

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        when(ruleVersionRepository
                .findTopByProgramIdOrderByVersionDesc(PROGRAM_ID))
                .thenReturn(Optional.empty());

        when(ruleVersionRepository.save(any(RuleVersion.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        ProgramRuleResponse response =
                programRuleService.updateRules(
                        PROGRAM_ID,
                        request
                );

        assertThat(response.getProgramId())
                .isEqualTo(PROGRAM_ID);

        assertThat(response.getRuleVersion())
                .isEqualTo(1);

        assertThat(response.getStatus())
                .isEqualTo(RuleStatus.SCHEDULED);

        assertThat(response.getEffectiveFrom())
                .isEqualTo(effectiveFrom);

        assertThat(response.getLockDays())
                .isEqualTo(7);

        assertThat(response.getExpiryDays())
                .isEqualTo(365);
    }

    @Test
    void updateRules_whenExistingRuleValidationFails_shouldNotMutateOrSave() {

        LoyaltyProgram program = buildProgram();

        RuleVersion currentRule =
                buildCurrentRule(program, 4);

        ProgramRuleRequest request =
                buildRequest(LocalDateTime.now().plusDays(5));

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(currentRule));

        doThrow(
                BusinessException.conflict(
                        ErrorCode.RULE_EFFECTIVE_PERIOD_CONFLICT,
                        "Conflict"
                )
        ).when(programRuleValidator)
                .validate(
                        eq(PROGRAM_ID),
                        eq(request),
                        eq(currentRule),
                        any(LocalDateTime.class)
                );

        assertThatThrownBy(
                () -> programRuleService.updateRules(
                        PROGRAM_ID,
                        request
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(throwable -> {

                    BusinessException exception =
                            (BusinessException) throwable;

                    assertThat(exception.getCode())
                            .isEqualTo(
                                    ErrorCode.RULE_EFFECTIVE_PERIOD_CONFLICT
                            );
                });

        verify(ruleVersionRepository, never())
                .save(any(RuleVersion.class));

        assertThat(currentRule.getEffectiveTo())
                .isNull();

        assertThat(currentRule.getStatus())
                .isEqualTo(RuleStatus.ACTIVE);
    }

    @Test
    void updateRules_whenFirstRuleValidationFails_shouldNotSave() {

        LoyaltyProgram program = buildProgram();

        ProgramRuleRequest request =
                buildRequest(LocalDateTime.now().plusDays(1));

        when(loyaltyProgramRepository.findByIdForUpdate(PROGRAM_ID))
                .thenReturn(Optional.of(program));

        when(ruleVersionRepository.findEffectiveRuleForUpdate(
                eq(PROGRAM_ID),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        doThrow(
                BusinessException.invalid(
                        ErrorCode.INVALID_LOCK_DAYS,
                        "Invalid lock days"
                )
        ).when(programRuleValidator)
                .validateFirstRule(
                        eq(PROGRAM_ID),
                        eq(request),
                        any(LocalDateTime.class)
                );

        assertThatThrownBy(
                () -> programRuleService.updateRules(
                        PROGRAM_ID,
                        request
                )
        )
                .isInstanceOf(BusinessException.class);

        verify(ruleVersionRepository, never())
                .save(any(RuleVersion.class));

        verify(ruleVersionRepository, never())
                .findTopByProgramIdOrderByVersionDesc(anyLong());
    }
}