package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.dto.response.*;
import com.aman.acceptance.loyalty.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/balance-composition")
    public ResponseEntity<ApiResponseDto<BalanceOverviewResponse>> getBalanceComposition() {
        BalanceOverviewResponse data = adminDashboardService.getBalanceOverview();
        return ResponseEntity.ok(new ApiResponseDto<>(true, data));
    }

    @GetMapping("/points-flow")
    public ResponseEntity<ApiResponseDto<List<PointsFlowResponse>>> getPointsFlow() {
        List<PointsFlowResponse> data = adminDashboardService.getPointsFlow();
        return ResponseEntity.ok(new ApiResponseDto<>(true, data));
    }

    @GetMapping("/otp-funnel")
    public ResponseEntity<ApiResponseDto<OtpFunnelResponse>> getOtpFunnel() {
        OtpFunnelResponse data = adminDashboardService.getOtpFunnel();
        return ResponseEntity.ok(new ApiResponseDto<>(true, data));
    }
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponseDto<List<AuditEventResponse>>> getRecentAlerts(
            @RequestParam(defaultValue = "10") int limit) {
        List<AuditEventResponse> data = adminDashboardService.getRecentAlerts(limit);
        return ResponseEntity.ok(new ApiResponseDto<>(true, data));
    }

}