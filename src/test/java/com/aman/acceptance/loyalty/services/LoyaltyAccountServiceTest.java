package com.aman.acceptance.loyalty.services;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.exceptions.AccountException;
import com.aman.acceptance.loyalty.model.Customer;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.repositries.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repositries.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repositries.PointsLotRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.aman.acceptance.loyalty.enums.AccountStatus.ACTIVE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class LoyaltyAccountServiceTest {

@Autowired
private LoyaltyAccountService loyaltyAccountService;

    @MockitoBean
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @MockitoBean
    private LoyaltyTransactionRepository transactionRepository;

    @MockitoBean
    private PointsLotRepository pointsLotRepository;

@Test
public void findAccountWhichIsFoundTest() {
    /**
     * precondition
     */

    final Long accountId = 1L;

    final Customer customer = Customer.builder().id(10L).mobileEncrypted("01012345678").build();

    final LoyaltyProgram loyaltyProgram = LoyaltyProgram.builder().id(20L).build();

    final LoyaltyAccount loyaltyAccount = LoyaltyAccount.builder().customer(customer).id(accountId)
            .program(loyaltyProgram).availablePoints(100).lockedPoints(20).reservedPoints(10).status(ACTIVE).build();

    Mockito.when(loyaltyAccountRepository.findById(accountId)).thenReturn(Optional.of(loyaltyAccount));

    PointsLot pointsLot = PointsLot.builder().remainingPoints(50).expiresAt(LocalDateTime.now()).build();

    Mockito.when(pointsLotRepository
                    .findFirstByAccount_IdAndStatusAndRemainingPointsGreaterThanOrderByExpiresAtAscUnlockAtAsc(
                            accountId, LotStatus.AVAILABLE, 0)).thenReturn(Optional.of(pointsLot));

    /**
     * Action
     */

    var response = loyaltyAccountService.findAccount(accountId);

    /**
     * Assert
     */

    assertNotNull(response);
    assertEquals("1",response.getAccountId());
    assertEquals("10",response.getCustomerId());
    assertEquals("20",response.getProgramId());
    assertEquals("ACTIVE",response.getAccountStatus());
    assertNotNull(response.getMobileNumberMasked());
    assertNotNull(response.getConversion());
    assertNotNull(response.getNearestExpiry());

}

    @Test
    public void findAccountWhichIsNotFoundTest() {
        /**
         * precondition
         */

        final Long accountId = 999L;

        Mockito.when(loyaltyAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        /**
         * Assert,Action
         */
     final AccountException accountException =
             assertThrows(AccountException.class, () -> loyaltyAccountService.findAccount(accountId));

        assertEquals("this is Account Is Not Found", accountException.getMessage());
}

}


