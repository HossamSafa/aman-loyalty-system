package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.responses.AccountResponse;
import com.aman.acceptance.loyalty.model.responses.ApiResponse;
import com.aman.acceptance.loyalty.model.responses.TransactionPageResponse;
import com.aman.acceptance.loyalty.service.LoyaltyAccountService;
import com.aman.acceptance.loyalty.utilies.validators.LoyaltyAccounHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loyalty/accounts")
@RequiredArgsConstructor
public class LoyaltyAccountController {

    private final LoyaltyAccountService loyaltyAccountService;

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @PathVariable Long accountId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

       final AccountResponse response = loyaltyAccountService.findAccount(accountId);

        return ResponseEntity.ok(new ApiResponse<>(true, response, LoyaltyAccounHelper.buildMeta(correlationId)));
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<ApiResponse<TransactionPageResponse>> getTransactions(
            @PathVariable Long accountId, Pageable pageable,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        TransactionPageResponse response = loyaltyAccountService.getTransactions(accountId, pageable);

        return ResponseEntity.ok(new ApiResponse<>(true, response,LoyaltyAccounHelper.buildMeta(correlationId)));
    }


}