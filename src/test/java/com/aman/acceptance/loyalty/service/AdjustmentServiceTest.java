package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.AccountStatus;
import com.aman.acceptance.loyalty.enums.AdjustmentType;
import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.model.AuditEvent;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.request.AdjustmentRequest;
import com.aman.acceptance.loyalty.model.response.AdjustmentResponse;
import com.aman.acceptance.loyalty.repository.AuditEventRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjustmentServiceTest {

    @Mock private LoyaltyAccountRepository loyaltyAccountRepository;
    @Mock private LoyaltyTransactionRepository loyaltyTransactionRepository;
    @Mock private PointsLotRepository pointsLotRepository;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private AccountFreezeService accountFreezeService;

    private AdjustmentService adjustmentService;

    @BeforeEach
    void setUp() {
        adjustmentService = new AdjustmentService(
                loyaltyAccountRepository,
                loyaltyTransactionRepository,
                pointsLotRepository,
                auditEventRepository,
                accountFreezeService,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(adjustmentService, "defaultExpiryDays", 360);
    }

    private LoyaltyAccount activeAccountWith(int available) {
        return LoyaltyAccount.builder()
                .id(1L)
                .status(AccountStatus.ACTIVE)
                .availablePoints(available)
                .lockedPoints(1000)
                .reservedPoints(0)
                .build();
    }

    // CREDIT

    @Test
    void adjust_credit_whenAccountActive_shouldCreateLotAndIncreaseAvailable() {
        LoyaltyAccount account = activeAccountWith(2500);

        AdjustmentRequest request = new AdjustmentRequest();
        request.setType(AdjustmentType.CREDIT);
        request.setPoints(500);
        request.setReasonCode("SERVICE_RECOVERY");
        request.setActorId("ops-user-01");

        when(loyaltyAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class)))
                .thenAnswer(invocation -> {
                    LoyaltyTransaction t = invocation.getArgument(0);
                    t.setId(50L);
                    return t;
                });
        when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> {
                    AuditEvent e = invocation.getArgument(0);
                    e.setId(200L);
                    return e;
                });

        AdjustmentResponse response = adjustmentService.adjust(1L, request);

        assertThat(response.getAdjustmentId()).isEqualTo(50L);
        assertThat(response.getPoints()).isEqualTo(500);
        assertThat(response.getBalance().getAvailable()).isEqualTo(3000); // 2500 + 500
        assertThat(account.getAvailablePoints()).isEqualTo(3000);

        ArgumentCaptor<PointsLot> lotCaptor = ArgumentCaptor.forClass(PointsLot.class);
        verify(pointsLotRepository).save(lotCaptor.capture());
        assertThat(lotCaptor.getValue().getStatus()).isEqualTo(LotStatus.AVAILABLE);
        assertThat(lotCaptor.getValue().getOriginalPoints()).isEqualTo(500);
        assertThat(lotCaptor.getValue().getRemainingPoints()).isEqualTo(500);

        verify(accountFreezeService).assertAccountActive(account);
        verify(loyaltyAccountRepository).save(account);
    }

    // DEBIT

    @Test
    void adjust_debit_whenSufficientPoints_shouldConsumeLotFifoAndDecreaseAvailable() {
        LoyaltyAccount account = activeAccountWith(2500);

        PointsLot lot = PointsLot.builder()
                .id(1L)
                .account(account)
                .originalPoints(2500)
                .remainingPoints(2500)
                .status(LotStatus.AVAILABLE)
                .expiresAt(LocalDateTime.now().plusDays(300))
                .build();

        AdjustmentRequest request = new AdjustmentRequest();
        request.setType(AdjustmentType.DEBIT);
        request.setPoints(300);
        request.setReasonCode("CORRECTION");
        request.setActorId("ops-user-01");

        when(loyaltyAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(pointsLotRepository.findByAccountAndStatusOrderByExpiresAtAsc(account, LotStatus.AVAILABLE))
                .thenReturn(List.of(lot));
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class)))
                .thenAnswer(invocation -> {
                    LoyaltyTransaction t = invocation.getArgument(0);
                    t.setId(51L);
                    return t;
                });
        when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> {
                    AuditEvent e = invocation.getArgument(0);
                    e.setId(201L);
                    return e;
                });

        AdjustmentResponse response = adjustmentService.adjust(1L, request);

        assertThat(response.getPoints()).isEqualTo(-300);
        assertThat(response.getBalance().getAvailable()).isEqualTo(2200); // 2500 - 300
        assertThat(lot.getRemainingPoints()).isEqualTo(2200);

        verify(pointsLotRepository).save(lot);
    }

    @Test
    void adjust_debit_whenInsufficientPoints_shouldThrowAndNeverMutate() {
        LoyaltyAccount account = activeAccountWith(100);

        AdjustmentRequest request = new AdjustmentRequest();
        request.setType(AdjustmentType.DEBIT);
        request.setPoints(999999);
        request.setReasonCode("CORRECTION");
        request.setActorId("ops-user-01");

        when(loyaltyAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> adjustmentService.adjust(1L, request))
                .isInstanceOf(LoyaltyException.class)
                .extracting(ex -> ((LoyaltyException) ex).getCode())
                .isEqualTo(ErrorCode.LOYALTY_INSUFFICIENT_AVAILABLE_POINTS);

        verify(loyaltyTransactionRepository, never()).save(any());
        verify(loyaltyAccountRepository, never()).save(any());
        verify(pointsLotRepository, never()).save(any());
    }

    // Guard Checks

    @Test
    void adjust_whenAccountFrozen_shouldPropagateFrozenExceptionAndNeverMutate() {
        LoyaltyAccount account = activeAccountWith(2500);

        AdjustmentRequest request = new AdjustmentRequest();
        request.setType(AdjustmentType.CREDIT);
        request.setPoints(500);
        request.setReasonCode("X");
        request.setActorId("ops-user-01");

        when(loyaltyAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        doThrow(LoyaltyException.locked(ErrorCode.LOYALTY_ACCOUNT_FROZEN, "This loyalty account is temporarily frozen."))
                .when(accountFreezeService).assertAccountActive(account);

        assertThatThrownBy(() -> adjustmentService.adjust(1L, request))
                .isInstanceOf(LoyaltyException.class)
                .extracting(ex -> ((LoyaltyException) ex).getCode())
                .isEqualTo(ErrorCode.LOYALTY_ACCOUNT_FROZEN);

        verify(loyaltyTransactionRepository, never()).save(any());
        verify(loyaltyAccountRepository, never()).save(any());
    }

    @Test
    void adjust_whenAccountNotFound_shouldThrow() {
        when(loyaltyAccountRepository.findById(99L)).thenReturn(Optional.empty());

        AdjustmentRequest request = new AdjustmentRequest();
        request.setType(AdjustmentType.CREDIT);
        request.setPoints(500);
        request.setReasonCode("X");
        request.setActorId("ops-user-01");

        assertThatThrownBy(() -> adjustmentService.adjust(99L, request))
                .isInstanceOf(LoyaltyException.class)
                .extracting(ex -> ((LoyaltyException) ex).getCode())
                .isEqualTo(ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND);
    }
}