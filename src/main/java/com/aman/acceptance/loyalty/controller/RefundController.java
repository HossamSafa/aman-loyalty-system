package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.dto.request.RefundRequest;
import com.aman.acceptance.loyalty.model.dto.response.RefundResponse;
import com.aman.acceptance.loyalty.model.responses.ApiResponse;
import com.aman.acceptance.loyalty.service.RefundService;
import com.aman.acceptance.loyalty.utilies.validators.LoyaltyAccounHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<ApiResponse<RefundResponse>> processRefund(
            @Valid
            @RequestBody
            RefundRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        RefundResponse response = refundService.processRefund(request);

        return ResponseEntity.ok(new ApiResponse<>(true, response, LoyaltyAccounHelper.buildMeta(correlationId)));
    }
}