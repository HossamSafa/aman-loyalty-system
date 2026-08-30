package com.aman.acceptance.loyalty.services;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.request.AmountRequest;
import com.aman.acceptance.loyalty.model.request.EarningRequest;
import com.aman.acceptance.loyalty.model.responses.EarningResponse;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.service.LoyaltyEarningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoyaltyEarningServiceTest {

    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Mock
    private PointsLotRepository pointsLotRepository;

    @InjectMocks
    private LoyaltyEarningService loyaltyEarningService;

    @Test
    void earnPoints_shouldEarnPointsSuccessfully() {

       final EarningRequest request = new EarningRequest();

        request.setAccountId("4451");

        request.setSourceTransactionId("sale-20260727-00091");

        request.setTransactionTime(OffsetDateTime.parse("2026-07-27T07:15:20Z"));

        request.setChannel("POS");

        final AmountRequest amount = new AmountRequest();

        amount.setValue(new BigDecimal("1000.00"));

        amount.setCurrency("EGP");

        request.setAmount(amount);

      final LoyaltyAccount account = new LoyaltyAccount();

        account.setAvailablePoints(2500);
        account.setLockedPoints(1000);
        account.setReservedPoints(0);

        when(loyaltyTransactionRepository.findByIdempotencyKey("earn-001")).thenReturn(Optional.empty());

        when(loyaltyTransactionRepository.findBySourceTransactionId(request.getSourceTransactionId()))
                .thenReturn(Optional.empty());

        when(loyaltyAccountRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(account));

        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class))).thenAnswer(invocation -> {

           final LoyaltyTransaction savedTransaction = invocation.getArgument(0);

            savedTransaction.setId(7L);

            return savedTransaction;
        });

       final PointsLot pointsLot = new PointsLot();

        pointsLot.setOriginalPoints(1000);

        pointsLot.setRemainingPoints(1000);

        pointsLot.setStatus(LotStatus.LOCKED);

        pointsLot.setAccount(account);

        pointsLot.setUnlockAt(request.getTransactionTime().plusDays(30).toLocalDateTime());

        pointsLot.setExpiresAt(request.getTransactionTime().plusDays(360).toLocalDateTime());


        when(pointsLotRepository.save(any(PointsLot.class))).thenReturn(pointsLot);

        final EarningResponse response = loyaltyEarningService.earnPoints(request, "earn-001", "cor-001");

        assertNotNull(response);

        assertEquals(request.getSourceTransactionId(), response.getSourceTransactionId());

        assertEquals(1000L, response.getEarnedPoints());

        assertEquals("LOCKED", response.getPointsStatus());

        verify(loyaltyTransactionRepository).save(any(LoyaltyTransaction.class));

        verify(pointsLotRepository).save(any(PointsLot.class));

        verify(loyaltyAccountRepository).save(account);

        assertEquals(2000, account.getLockedPoints());
    }
}