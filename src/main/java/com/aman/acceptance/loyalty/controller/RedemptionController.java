package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.dto.request.RedemptionRequest;

import com.aman.acceptance.loyalty.model.dto.response.ApiResponse;
import com.aman.acceptance.loyalty.model.dto.response.RedemptionResponseData;
import com.aman.acceptance.loyalty.service.RedemptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
