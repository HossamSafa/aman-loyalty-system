package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.response.ApiResponse;
import com.aman.acceptance.loyalty.model.response.CustomerSummaryResponse;
import com.aman.acceptance.loyalty.model.response.PagedResponse;
import com.aman.acceptance.loyalty.service.CustomerSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final CustomerSearchService customerSearchService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CustomerSummaryResponse>>> list(
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        PagedResponse<CustomerSummaryResponse> result = customerSearchService.search(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

}
