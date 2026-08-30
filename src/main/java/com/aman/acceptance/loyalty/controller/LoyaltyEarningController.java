package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.request.EarningRequest;
import com.aman.acceptance.loyalty.model.response.ApiResponse;
import com.aman.acceptance.loyalty.model.responses.EarningResponse;
import com.aman.acceptance.loyalty.service.LoyaltyEarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class LoyaltyEarningController {

    private final LoyaltyEarningService loyaltyEarningService;

    @PostMapping("earnings")
    public  ResponseEntity<ApiResponse<EarningResponse>> earnPoints(
            @RequestBody EarningRequest earningRequest,
            @RequestHeader(value = "Idempotency-Key")
            String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false)
            String correlationId) {

        final EarningResponse response = loyaltyEarningService.earnPoints(earningRequest, idempotencyKey, correlationId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
