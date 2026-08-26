package com.aman.acceptance.loyalty.controller;

import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import com.aman.acceptance.loyalty.model.responses.ApiResponse;
import com.aman.acceptance.loyalty.model.dto.response.LoyaltyAccountResponseDto;
import com.aman.acceptance.loyalty.service.LoyaltyAccountService;
import com.aman.acceptance.loyalty.utilies.validators.LoyaltyAccounHelper;

import lombok.RequiredArgsConstructor;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final LoyaltyAccountService loyaltyAccountService;

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<Page<LoyaltyAccountResponseDto>>> getLoyaltyAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        PageRequest pageable = PageRequest.of(page, size);
        Page<LoyaltyAccountResponseDto> response = loyaltyAccountService.getEnrolledAccounts(pageable);

        return ResponseEntity.ok(new ApiResponse<>(true, response, LoyaltyAccounHelper.buildMeta(correlationId)));
    }
}
