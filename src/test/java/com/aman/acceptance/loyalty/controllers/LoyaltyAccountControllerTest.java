package com.aman.acceptance.loyalty.controllers;

import com.aman.acceptance.loyalty.model.responses.*;
import com.aman.acceptance.loyalty.services.LoyaltyAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class LoyaltyAccountControllerTest {
    @InjectMocks
    private LoyaltyAccountController loyaltyAccountController;

    @Mock
    private LoyaltyAccountService loyaltyAccountService;
    private TransactionPageResponse transactionPageResponse;
    private ApiResponse apiResponse;
    private AccountResponse accountResponse;


    @Test
    public void getAccountTest() {
        /*** Precindition */

        final Long accountId = 1l;

       final String correlationId = "X-Correlation-Id";

       final BalanceResponse balanceResponse = new BalanceResponse(30,10,10,50);

       final NearestExpiryResponse nearestExpiryResponse =
               new NearestExpiryResponse(40l, Instant.parse("2026-08-23T01:37:44Z"));

       final ConversionResponse conversionResponse =
               new ConversionResponse(new BigDecimal("0.005"),new BigDecimal("1"));

     final AccountResponse accountResponse =
              new AccountResponse("1","1","1","Active",
                      "aaaaa",balanceResponse,nearestExpiryResponse,conversionResponse);

        Mockito.when(loyaltyAccountService.findAccount(any())).thenReturn(accountResponse);

        /**
         * Action
         */
        final ResponseEntity<?> responseEntity =
                loyaltyAccountController.getAccount(accountId,correlationId);

        final ApiResponse<AccountResponse> apiResponse =
                (ApiResponse<AccountResponse>) responseEntity.getBody();

        /**
         * Assert
         */
        assertNotNull(responseEntity);

        assertEquals(200,responseEntity.getStatusCode().value());

        assertNotNull(responseEntity.getBody());
    }

    @Test
    /*** Precindition */
    public void getTransactionsTest() {
       final Long accountId = 1l;

       final Pageable pageable = Pageable.ofSize(20);

       final String correlationId = "X-Correlation-Id";

       final TransactionPageResponse transactionPageResponse =
                new TransactionPageResponse(java.util.List.of(),0,20,0,0);

        Mockito.when(loyaltyAccountService.getTransactions(any(),any())).thenReturn(transactionPageResponse);

        /**
         * Action
         */

               final ResponseEntity<?> responseEntity =
                         loyaltyAccountController.getTransactions(accountId,pageable,correlationId);

                final ApiResponse<TransactionPageResponse> apiResponse =
                          (ApiResponse<TransactionPageResponse>) responseEntity.getBody();

        /**
         * Assert
         */
        assertNotNull(responseEntity);

        assertEquals(200,responseEntity.getStatusCode().value());

        assertNotNull(responseEntity.getBody());

        assertEquals(transactionPageResponse,apiResponse.getData());
    }

}
