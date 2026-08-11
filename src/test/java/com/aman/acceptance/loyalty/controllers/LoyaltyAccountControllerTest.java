package com.aman.acceptance.loyalty.controllers;


import com.aman.acceptance.loyalty.model.responses.AccountResponse;
import com.aman.acceptance.loyalty.model.responses.TransactionPageResponse;
import com.aman.acceptance.loyalty.services.LoyaltyAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyAccountControllerTest {

    @Mock
    private LoyaltyAccountService loyaltyAccountService;

    @InjectMocks
    private LoyaltyAccountController loyaltyAccountController;

    private AccountResponse accountResponse;
    private TransactionPageResponse transactionPageResponse;

    @BeforeEach
    void setUp() {

        accountResponse = new AccountResponse("1", "1", "1", "ACTIVE",
                null, null, null, null);

        transactionPageResponse =
                new TransactionPageResponse(java.util.List.of(), 0, 20, 0, 0);
    }

    @Test
    void getAccount_shouldReturn200() {when(loyaltyAccountService.getAccount(1L)).thenReturn(accountResponse);

        ResponseEntity<?> response = loyaltyAccountController.getAccount(1L, "cor-7d8f2c");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void getTransactions_shouldReturn200() {

        when(loyaltyAccountService.getTransactions(any(Long.class), any())).thenReturn(transactionPageResponse);

        ResponseEntity<?> response = loyaltyAccountController.getTransactions(1L,
                        PageRequest.of(0, 20), "cor-7d8f2c");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
}
