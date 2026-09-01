package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.request.AdjustmentRequest;
import com.aman.acceptance.loyalty.model.request.FreezeAccountRequest;
import com.aman.acceptance.loyalty.model.request.UnfreezeAccountRequest;
import com.aman.acceptance.loyalty.model.response.*;
import com.aman.acceptance.loyalty.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AccountFreezeService accountFreezeService;
    private final AdjustmentService adjustmentService;
    private final RiskSummaryService riskSummaryService;
    private final ActivityFeedService activityFeedService;
    private final AccountSummaryService accountSummaryService;

    @PostMapping("/{accountId}/freeze")
    public ResponseEntity<ApiResponse<AccountStatusResponse>> freeze(
            @PathVariable Long accountId,
            @Valid @RequestBody FreezeAccountRequest request
            ){
        AccountStatusResponse response = accountFreezeService.freeze(accountId, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }

    @PostMapping("{accountId}/unfreeze")
    public ResponseEntity<ApiResponse<AccountStatusResponse>> unfreeze(
            @PathVariable Long accountId,
            @Valid @RequestBody UnfreezeAccountRequest request
            ){
        AccountStatusResponse response = accountFreezeService.unfreeze(accountId, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }

    @PostMapping("/{accountId}/adjustments")
    public ResponseEntity<ApiResponse<AdjustmentResponse>> adjust(
            @PathVariable Long accountId,
            @Valid @RequestBody AdjustmentRequest request
    ) {
        AdjustmentResponse response = adjustmentService.adjust(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/risk-summary")
    public ResponseEntity<ApiResponse<RiskSummaryResponse>> riskSummary() {
        return ResponseEntity.ok(ApiResponse.success(riskSummaryService.getSummary()));
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<ActivityFeedItemResponse>>> recentActivity() {
        return ResponseEntity.ok(ApiResponse.success(activityFeedService.getRecentActivity()));
    }

    @GetMapping("/{accountId}/summary")
    public ResponseEntity<ApiResponse<AccountSummaryResponse>> summary(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success(accountSummaryService.getSummary(accountId)));
    }
}
