package com.aman.acceptance.loyalty.services;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.exception.AccountException;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.responses.ExpireBatchResult;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.service.LoyaltyPointsLifecycleService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class LoyaltyPointsLifecycleServiceTest {

    @Autowired
    private LoyaltyPointsLifecycleService loyaltyPointsLifecycleService;

   @MockitoBean
    private PointsLotRepository pointsLotRepository;

    @MockitoBean
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @MockitoBean
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Test
    @Transactional
    public void processExpire_shouldExpireLotsAndReturnBatchResult() throws AccountException {

        //PRECONDITION

       final Pageable pageable = PageRequest.of(0, 1000);

       final LocalDateTime expiresAt = LocalDateTime.of(2026, 8, 19, 23, 59, 59);

       final PointsLot pointsLot = new PointsLot();

        pointsLot.setId(1L);

        pointsLot.setRemainingPoints(500);

        final LoyaltyAccount loyaltyAccount = new LoyaltyAccount();

        loyaltyAccount.setAvailablePoints(1000);

        pointsLot.setAccount(loyaltyAccount);

        final List<PointsLot> lots = List.of(pointsLot);

        Mockito.when(pointsLotRepository

                .findByStatusAndExpiresAtLessThanEqualAndRemainingPointsGreaterThanAndIdGreaterThanOrderByIdAsc(

                        LotStatus.AVAILABLE, expiresAt, 0, 0L, pageable)).thenReturn(lots);

        Mockito.when(loyaltyAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loyaltyAccount));

        Mockito.when(loyaltyTransactionRepository

                .findBySourceTransactionIdAndType("LOT-1", TransactionType.EXPIRE)).thenReturn(Optional.empty());

        // Action

       final ExpireBatchResult result =

                loyaltyPointsLifecycleService.processExpire(pageable, LotStatus.AVAILABLE, expiresAt, 0, 0L);

        // Assert

        assertEquals("EXPIRE_POINTS", result.getJobName());

        assertEquals(1, result.getProcessedLots());

        assertEquals(500, result.getExpiredPoints());

        assertEquals("lot-1", result.getNextCheckpoint());

        assertEquals(500, loyaltyAccount.getAvailablePoints());

        assertEquals(0, pointsLot.getRemainingPoints());

        assertEquals(LotStatus.EXPIRED, pointsLot.getStatus());

        verify(loyaltyTransactionRepository).save(any(LoyaltyTransaction.class));

    }


}
