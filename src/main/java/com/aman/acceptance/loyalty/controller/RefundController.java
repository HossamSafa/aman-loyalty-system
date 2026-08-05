package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.dto.request.RefundRequest;
import com.aman.acceptance.loyalty.dto.response.RefundResponse;
import com.aman.acceptance.loyalty.service.RefundService;
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
    public ResponseEntity<RefundResponse> processRefund(
            @Valid
            @RequestBody
            RefundRequest request
    )
    {
        RefundResponse response =
                refundService.processRefund(request);

        return ResponseEntity.ok(response);
    }
}