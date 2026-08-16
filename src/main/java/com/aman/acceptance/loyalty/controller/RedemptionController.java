package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.dto.request.RedemptionRequest;

import com.aman.acceptance.loyalty.model.dto.request.VerifyOtpRequest;
import com.aman.acceptance.loyalty.model.dto.response.ApiResponse;
import com.aman.acceptance.loyalty.model.dto.response.RedemptionResponseData;
import com.aman.acceptance.loyalty.model.dto.response.VerifyRedemptionResponseData;
import com.aman.acceptance.loyalty.service.RedemptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aman.acceptance.loyalty.model.dto.request.CommitRequest;
import com.aman.acceptance.loyalty.model.dto.request.CancelRequest;
import com.aman.acceptance.loyalty.model.dto.response.CommitResponseData;
import com.aman.acceptance.loyalty.model.dto.response.CancelResponseData;

@RestController
@RequestMapping("/redemptions")
public class RedemptionController {

    private final RedemptionService redemptionService;

    public RedemptionController(RedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RedemptionResponseData>> initiateRedemption(
            @Valid @RequestBody RedemptionRequest request) {
        


        RedemptionResponseData responseData = redemptionService.initiateRedemption(request);

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<VerifyRedemptionResponseData>> verifyOtp(
            @PathVariable Long id,
            @Valid @RequestBody VerifyOtpRequest request) {

        VerifyRedemptionResponseData responseData = redemptionService.verifyRedemption(id, request.otp());

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @PostMapping("/{id}/commit")
    public ResponseEntity<ApiResponse<CommitResponseData>> commitRedemption(
            @PathVariable Long id,
            @Valid @RequestBody CommitRequest request) {

        CommitResponseData responseData = redemptionService.commitRedemption(id, request);

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<CancelResponseData>> cancelRedemption(
            @PathVariable Long id,
            @Valid @RequestBody CancelRequest request) {

        CancelResponseData responseData = redemptionService.cancelRedemption(id, request);

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }
}
