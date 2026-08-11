package com.aman.acceptance.loyalty.services;


import com.aman.acceptance.loyalty.enums.AccountStatus;
import com.aman.acceptance.loyalty.exceptions.ResourceNotFoundException;
import com.aman.acceptance.loyalty.model.Customer;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import com.aman.acceptance.loyalty.repositries.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repositries.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repositries.PointsLotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class LoyaltyAccountServiceTest {

    @Mock
    private LoyaltyAccountRepository accountRepository;

    @Mock
    private LoyaltyTransactionRepository transactionRepository;

    @Mock
    private PointsLotRepository pointsLotRepository;

    @InjectMocks
    private LoyaltyAccountService loyaltyAccountService;

    @Test
    void getAccount_shouldReturnAccountResponse() {

        // Arrange
        Long accountId = 1L;

        Customer customer = Customer.builder().id(10L).build();

        LoyaltyProgram program = LoyaltyProgram.builder().id(20L).build();

        LoyaltyAccount account = LoyaltyAccount.builder().id(accountId).customer(customer).program(program)
                .availablePoints(100).lockedPoints(20).reservedPoints(10).status(AccountStatus.ACTIVE).build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        when(pointsLotRepository
                .findFirstByAccount_IdAndStatusAndRemainingPointsGreaterThanOrderByExpiresAtAscUnlockAtAsc(
                        anyLong(), any(), anyInt())).thenReturn(Optional.empty());

        // Act
        var response = loyaltyAccountService.findAccount(accountId);

        // Assert
        assertNotNull(response);

        assertEquals("1", response.getAccountId());
        assertEquals("10", response.getCustomerId());
        assertEquals("20", response.getProgramId());
        assertEquals("ACTIVE", response.getAccountStatus());

        assertNotNull(response.getBalance());

        assertEquals(100L, response.getBalance().getAvailable());
        assertEquals(20L, response.getBalance().getLocked());
        assertEquals(10L, response.getBalance().getReserved());
        assertEquals(130L, response.getBalance().getTotalOwned());

        verify(accountRepository).findById(accountId);
    }

    @Test
    void getAccount_shouldThrowException_whenAccountNotFound() {

        // Arrange
        Long accountId = 999L;

        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> loyaltyAccountService.findAccount(accountId));

        verify(accountRepository).findById(accountId);
    }
}