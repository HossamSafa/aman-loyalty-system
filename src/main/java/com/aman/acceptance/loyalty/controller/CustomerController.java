package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.dto.request.ResolveCustomerRequest;
import com.aman.acceptance.loyalty.model.dto.response.ResolveCustomerApiResponse;
import com.aman.acceptance.loyalty.model.dto.response.ResolveCustomerResponse;
import com.aman.acceptance.loyalty.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@Slf4j
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/resolve")
    public ResponseEntity<ResolveCustomerApiResponse> resolveCustomer(
        @Valid @RequestBody ResolveCustomerRequest request,
        @RequestHeader("X-Merchant-Program-Id") Long programId,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {

        log.info("Received request with Idempotency-Key: {}", idempotencyKey);

        ResolveCustomerResponse resolveCustomerResponse = customerService.resolve(request, programId);
        ResolveCustomerApiResponse apiResponse = new ResolveCustomerApiResponse(true, resolveCustomerResponse);
        return ResponseEntity.ok(apiResponse);
    }

}