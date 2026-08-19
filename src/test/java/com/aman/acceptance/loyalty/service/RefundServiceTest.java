package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.*;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.model.*;
import com.aman.acceptance.loyalty.model.dto.MoneyDto;
import com.aman.acceptance.loyalty.model.dto.request.RefundRequest;
import com.aman.acceptance.loyalty.model.dto.response.RefundResponse;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.repository.RedemptionRepository;
import com.aman.acceptance.loyalty.service.calculation.RefundCalculation;
import com.aman.acceptance.loyalty.service.calculation.RefundCalculator;
import com.aman.acceptance.loyalty.service.validators.refundValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private LoyaltyTransactionRepository transactionRepository;
    @Mock
    private PointsLotRepository pointsLotRepository;
    @Mock
    private LoyaltyAccountRepository accountRepository;
    @Mock
    private RefundCalculator refundCalculator;
    @Mock
    private refundValidator refundValidator;
    @Mock
    private RedemptionRepository redemptionRepository;

    @InjectMocks
    private RefundService refundService;

    private LoyaltyAccount account;
    private LoyaltyTransaction originalTransaction;
    private PointsLot pointsLot;
    private RefundRequest request;

    @BeforeEach
    void setUp() {
        account = LoyaltyAccount.builder()
                .id(7L)
                .availablePoints(0)
                .lockedPoints(1000)
                .reservedPoints(0)
                .build();

        originalTransaction = LoyaltyTransaction.builder()
                .id(4L)
                .account(account)
                .type(TransactionType.EARN)
                .sourceTransactionId("sale-test-001")
                .points(1000)
                .moneyAmount(new BigDecimal("1000.00"))
                .status(TransactionStatus.COMMITTED)
                .build();

        pointsLot = PointsLot.builder()
                .id(3L)
                .account(account)
                .earningTransaction(originalTransaction)
                .originalPoints(1000)
                .remainingPoints(1000)
                .status(LotStatus.LOCKED)
                .unlockAt(LocalDateTime.now().plusDays(1))
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build();

        request = RefundRequest.builder()
                .refundTransactionId("refund-test-003")
                .originalTransactionId("sale-test-001")
                .redemptionId(1L)
                .refundType(RefundType.FULL)
                .refundAmount(MoneyDto.builder()
                        .value(new BigDecimal("1000.00"))
                        .currency(CurrencyCode.EGP)
                        .build())
                .refundTime(LocalDateTime.now())
                .build();
    }

    @Test
    void processRefund_shouldCompleteFullRefundWithRedemptionRestore_whenValidRequest() {
        // Arrange
        PointsLot redemptionLot = PointsLot.builder()
                .id(3L)
                .remainingPoints(0)
                .build();

        Redemption redemption = Redemption.builder()
                .id(1L)
                .account(account)
                .requestedPoints(1000)
                .discountAmount(new BigDecimal("100.00"))
                .status(RedemptionStatus.COMMITTED)
                .build();
        redemption.addAllocation(RedemptionAllocation.builder()
                .id(1L)
                .lot(redemptionLot)
                .points(1000)
                .build());

        when(transactionRepository.findBySourceTransactionIdAndType(
                "refund-test-003", TransactionType.REFUND_EARN_REVERSAL))
                .thenReturn(Optional.empty());

        when(transactionRepository.findBySourceTransactionIdAndType(
                "sale-test-001", TransactionType.EARN))
                .thenReturn(Optional.of(originalTransaction));

        when(accountRepository.findByIdForUpdate(7L))
                .thenReturn(Optional.of(account));

        when(pointsLotRepository.findByEarningTransactionIdForUpdate(4L))
                .thenReturn(Optional.of(pointsLot));

        when(transactionRepository.findAllByAccount_IdAndOriginalSourceTransactionIdAndTypeAndStatus(
                7L, "sale-test-001", TransactionType.REFUND_EARN_REVERSAL, TransactionStatus.COMMITTED))
                .thenReturn(List.of());

        when(refundCalculator.calculate(
                any(), anyInt(), any(), anyInt(), any(), any()))
                .thenReturn(new RefundCalculation(new BigDecimal("1000.00"), 1000));

        when(redemptionRepository.findByIdAndAccount_Id(1L, 7L))
                .thenReturn(Optional.of(redemption));

        // Act
        RefundResponse response = refundService.processRefund(request);

        // Assert
        assertThat(response.getReversedEarnedPoints()).isEqualTo(1000);
        assertThat(response.getRestoredRedeemedPoints()).isEqualTo(1000);
        assertThat(response.getRestoredRedemptionValue().getValue())
                .isEqualByComparingTo("100.00");
        assertThat(response.getBalance().getAvailable()).isEqualTo(1000);
        assertThat(response.getBalance().getLocked()).isEqualTo(0);
        assertThat(pointsLot.getRemainingPoints()).isEqualTo(0);
        assertThat(pointsLot.getStatus()).isEqualTo(LotStatus.CANCELLED);

        verify(transactionRepository).save(any(LoyaltyTransaction.class));
    }

    @Test
    void processRefund_shouldReturnExistingResponse_whenRefundTransactionIdAlreadyExists() {
        LoyaltyTransaction existingRefund = LoyaltyTransaction.builder()
                .id(10L)
                .account(account)
                .type(TransactionType.REFUND_EARN_REVERSAL)
                .sourceTransactionId("refund-test-003")
                .originalSourceTransactionId("sale-test-001")
                .points(-1000)
                .moneyAmount(new BigDecimal("1000.00"))
                .currency(CurrencyCode.EGP)
                .refundType(RefundType.FULL)
                .build();

        when(transactionRepository.findBySourceTransactionIdAndType(
                "refund-test-003", TransactionType.REFUND_EARN_REVERSAL))
                .thenReturn(Optional.of(existingRefund));

        RefundResponse response = refundService.processRefund(request);

        assertThat(response.getRestoredRedeemedPoints()).isEqualTo(0);
        assertThat(response.getReversedEarnedPoints()).isEqualTo(1000);
        verify(refundValidator).validateSameRequest(existingRefund, request);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void processRefund_shouldThrowNotFound_whenOriginalTransactionNotFound() {
        when(transactionRepository.findBySourceTransactionIdAndType(
                "refund-test-003", TransactionType.REFUND_EARN_REVERSAL))
                .thenReturn(Optional.empty());

        when(transactionRepository.findBySourceTransactionIdAndType(
                "sale-test-001", TransactionType.EARN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundService.processRefund(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Original earning transaction was not found.");

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void processRefund_shouldThrowNotFound_whenRedemptionNotFound() {
        when(transactionRepository.findBySourceTransactionIdAndType(
                "refund-test-003", TransactionType.REFUND_EARN_REVERSAL))
                .thenReturn(Optional.empty());

        when(transactionRepository.findBySourceTransactionIdAndType(
                "sale-test-001", TransactionType.EARN))
                .thenReturn(Optional.of(originalTransaction));

        when(accountRepository.findByIdForUpdate(7L))
                .thenReturn(Optional.of(account));

        when(pointsLotRepository.findByEarningTransactionIdForUpdate(4L))
                .thenReturn(Optional.of(pointsLot));

        when(transactionRepository.findAllByAccount_IdAndOriginalSourceTransactionIdAndTypeAndStatus(
                7L, "sale-test-001", TransactionType.REFUND_EARN_REVERSAL, TransactionStatus.COMMITTED))
                .thenReturn(List.of());

        when(refundCalculator.calculate(any(), anyInt(), any(), anyInt(), any(), any()))
                .thenReturn(new RefundCalculation(new BigDecimal("1000.00"), 1000));

        when(redemptionRepository.findByIdAndAccount_Id(1L, 7L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundService.processRefund(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Redemption was not found for this account.");
    }
}