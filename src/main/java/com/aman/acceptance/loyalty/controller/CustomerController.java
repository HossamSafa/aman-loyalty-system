package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.dto.request.CustomerRequestDto;
import com.aman.acceptance.loyalty.model.dto.response.CustomerResponseDto;
import com.aman.acceptance.loyalty.model.dto.response.CustomerDto;
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
    public ResponseEntity<CustomerResponseDto> resolveCustomer(
        @Valid @RequestBody CustomerRequestDto request,
        @RequestHeader("X-Merchant-Program-Id") Long programId,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {

        log.info("Received request with Idempotency-Key: {}", idempotencyKey);

        CustomerDto customerDto = customerService.resolve(request, programId);
        CustomerResponseDto apiResponse = new CustomerResponseDto(true, customerDto);
        return ResponseEntity.ok(apiResponse);
    }

}