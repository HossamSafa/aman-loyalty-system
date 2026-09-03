package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.AccountStatus;
import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.model.AuditEvent;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.request.FreezeAccountRequest;
import com.aman.acceptance.loyalty.model.request.UnfreezeAccountRequest;
import com.aman.acceptance.loyalty.model.response.AccountStatusResponse;
import com.aman.acceptance.loyalty.repository.AuditEventRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountFreezeServiceTest {

    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    private AccountFreezeService accountFreezeService;

    @Mock
    private AccountStatusGuard accountStatusGuard;

    @Mock
    private RedemptionService redemptionService;

    @BeforeEach
    void setUp() {
        accountFreezeService = new AccountFreezeService(
                loyaltyAccountRepository,
                auditEventRepository,
                new ObjectMapper(),
                accountStatusGuard,
                redemptionService
        );
    }

    // ==================== freeze() ====================

    @Test
    void freeze_whenAccountIsActive_shouldFreezeAndCreateAuditEvent() {
        LoyaltyAccount account = LoyaltyAccount.builder()
                .id(1L)
                .status(AccountStatus.ACTIVE)
                .availablePoints(2500)
                .lockedPoints(1000)
                .reservedPoints(0)
                .build();

        FreezeAccountRequest request = new FreezeAccountRequest();
        request.setReasonCode("SUSPICIOUS_REDEMPTION_PATTERN");
        request.setNote("Multiple OTP failures");
        request.setActorId("fraud-analyst-01");

        when(loyaltyAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> {
                    AuditEvent event = invocation.getArgument(0);
                    event.setId(100L);
                    return event;
                });

        AccountStatusResponse response = accountFreezeService.freeze(1L, request);

        assertThat(response.getStatus()).isEqualTo("FROZEN");
        assertThat(response.getAccountId()).isEqualTo(1L);
        assertThat(response.getAuditId()).isEqualTo(100L);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.FROZEN);

        verify(loyaltyAccountRepository).save(account);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("FREEZE");
        assertThat(auditCaptor.getValue().getActorId()).isEqualTo("fraud-analyst-01");
    }

    @Test
    void freeze_whenAccountAlreadyFrozen_shouldThrowConflictAndNeverSave() {
        LoyaltyAccount account = LoyaltyAccount.builder().id(1L).status(AccountStatus.FROZEN).build();
        when(loyaltyAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        FreezeAccountRequest request = new FreezeAccountRequest();
        request.setReasonCode("X");
        request.setActorId("fraud-analyst-01");

        assertThatThrownBy(() -> accountFreezeService.freeze(1L, request))
                .isInstanceOf(LoyaltyException.class)
                .extracting(ex -> ((LoyaltyException) ex).getCode())
                .isEqualTo(ErrorCode.LOYALTY_ACCOUNT_ALREADY_FROZEN);

        verify(loyaltyAccountRepository, never()).save(any());
        verify(auditEventRepository, never()).save(any());
    }

    @Test
    void freeze_whenAccountNotFound_shouldThrowNotFound() {
        when(loyaltyAccountRepository.findById(99L)).thenReturn(Optional.empty());

        FreezeAccountRequest request = new FreezeAccountRequest();
        request.setReasonCode("X");
        request.setActorId("fraud-analyst-01");

        assertThatThrownBy(() -> accountFreezeService.freeze(99L, request))
                .isInstanceOf(LoyaltyException.class)
                .extracting(ex -> ((LoyaltyException) ex).getCode())
                .isEqualTo(ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND);
    }

    // ==================== unfreeze() ====================

    @Test
    void unfreeze_whenAccountIsFrozen_shouldUnfreezeAndCreateAuditEvent() {
        LoyaltyAccount account = LoyaltyAccount.builder()
                .id(1L)
                .status(AccountStatus.FROZEN)
                .availablePoints(2500)
                .lockedPoints(1000)
                .reservedPoints(0)
                .build();

        UnfreezeAccountRequest request = new UnfreezeAccountRequest();
        request.setReasonCode("REVIEW_COMPLETED");
        request.setActorId("fraud-analyst-01");

        when(loyaltyAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> {
                    AuditEvent event = invocation.getArgument(0);
                    event.setId(101L);
                    return event;
                });

        AccountStatusResponse response = accountFreezeService.unfreeze(1L, request);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(loyaltyAccountRepository).save(account);
    }

    @Test
    void unfreeze_whenAccountNotFrozen_shouldThrowConflictAndNeverSave() {
        LoyaltyAccount account = LoyaltyAccount.builder().id(1L).status(AccountStatus.ACTIVE).build();
        when(loyaltyAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        UnfreezeAccountRequest request = new UnfreezeAccountRequest();
        request.setReasonCode("X");
        request.setActorId("fraud-analyst-01");

        assertThatThrownBy(() -> accountFreezeService.unfreeze(1L, request))
                .isInstanceOf(LoyaltyException.class)
                .extracting(ex -> ((LoyaltyException) ex).getCode())
                .isEqualTo(ErrorCode.LOYALTY_ACCOUNT_NOT_FROZEN);

        verify(loyaltyAccountRepository, never()).save(any());
    }

    @Test
    void unfreeze_whenAccountNotFound_shouldThrowNotFound() {
        when(loyaltyAccountRepository.findById(99L)).thenReturn(Optional.empty());

        UnfreezeAccountRequest request = new UnfreezeAccountRequest();
        request.setReasonCode("X");
        request.setActorId("fraud-analyst-01");

        assertThatThrownBy(() -> accountFreezeService.unfreeze(99L, request))
                .isInstanceOf(LoyaltyException.class)
                .extracting(ex -> ((LoyaltyException) ex).getCode())
                .isEqualTo(ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND);
    }

    // ==================== assertAccountActive() ====================

    @Test
    void assertAccountActive_whenAccountIsActive_shouldNotThrow() {
        LoyaltyAccount account = LoyaltyAccount.builder().id(1L).status(AccountStatus.ACTIVE).build();

        assertThatCode(() -> accountFreezeService.assertAccountActive(account))
                .doesNotThrowAnyException();
    }

    @Test
    void assertAccountActive_whenAccountIsFrozen_shouldThrowLockedException() {
        LoyaltyAccount account = LoyaltyAccount.builder().id(2L).status(AccountStatus.FROZEN).build();

        assertThatThrownBy(() -> accountFreezeService.assertAccountActive(account))
                .isInstanceOf(LoyaltyException.class)
                .extracting(ex -> ((LoyaltyException) ex).getCode())
                .isEqualTo(ErrorCode.LOYALTY_ACCOUNT_FROZEN);
    }
}