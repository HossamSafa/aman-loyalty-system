package com.aman.acceptance.loyalty.security;


import com.aman.acceptance.loyalty.model.Customer;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import com.aman.acceptance.loyalty.repositries.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repositries.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repositries.PointsLotRepository;
import com.aman.acceptance.loyalty.services.LoyaltyAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

@ExtendWith(MockitoExtension.class)

public class JwtAuthenticationConverterTest {
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

        Long accountId = 1L;

        Customer customer = Customer.builder()
                .id(10L)
                .build();

        LoyaltyProgram program = LoyaltyProgram.builder()
                .id(20L)
                .build();

        LoyaltyAccount account = LoyaltyAccount.builder()
                .id(accountId)
                .customer(customer)
                .program(program)
                .availablePoints(100)
                .lockedPoints(20)
                .reservedPoints(10)
                .build();

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(pointsLotRepository
                .findFirstByAccount_IdAndStatusAndRemainingPointsGreaterThanOrderByExpiresAtAscUnlockAtAsc(
                        anyLong(),
                        any(),
                        anyInt()
                ))
                .thenReturn(Optional.empty());

        var response =
                loyaltyAccountService.getAccount(accountId);

        assertNotNull(response);

        assertEquals("1", response.accountId());
        assertEquals("10", response.customerId());
        assertEquals("20", response.programId());

        assertEquals(100L, response.balance().available());
        assertEquals(20L, response.balance().locked());
        assertEquals(10L, response.balance().reserved());
        assertEquals(130L, response.balance().totalOwned());

        verify(accountRepository).findById(accountId);
    }

    @Test
    void getAccount_shouldThrowException_whenAccountNotFound() {

        Long accountId = 999L;

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> loyaltyAccountService.getAccount(accountId)
        );

        verify(accountRepository).findById(accountId);
    }
}
