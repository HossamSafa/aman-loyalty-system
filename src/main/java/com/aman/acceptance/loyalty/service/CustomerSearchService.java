package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.Customer;
import com.aman.acceptance.loyalty.model.response.CustomerSummaryResponse;
import com.aman.acceptance.loyalty.model.response.PagedResponse;
import com.aman.acceptance.loyalty.repository.CustomerRepository;
import com.aman.acceptance.loyalty.util.MobileHashUtil;
import org.springframework.data.domain.PageImpl;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerSearchService {

    private final CustomerRepository customerRepository;

    private static final String MOBILE_PATTERN = "^[+0-9]+$";

    public PagedResponse<CustomerSummaryResponse> search(String search, Pageable pageable) {

        Page<Customer> customerPage;

        if (search == null || search.isBlank()) {
            customerPage = customerRepository.findAll(pageable);
        } else {
            String normalizedSearch = search.trim().replace(" ", "+");

            if (normalizedSearch.matches(MOBILE_PATTERN)) {
                customerPage = searchByMobile(normalizedSearch, pageable);
            } else {
                customerPage = customerRepository.findByNameContainingIgnoreCase(search, pageable);
            }
        }

        Page<CustomerSummaryResponse> mapped = customerPage.map(this::toSummary);
        return PagedResponse.from(mapped);
    }

    private Page<Customer> searchByMobile(String rawMobile, Pageable pageable) {
        String normalized = MobileHashUtil.normalizeMobile(rawMobile);
        String hash = MobileHashUtil.hashMobile(normalized);

        Optional<Customer> match = customerRepository.findByMobileHash(hash);
        List<Customer> results = match.map(List::of).orElse(List.of());

        return new PageImpl<>(results, pageable, results.size());
    }

    private CustomerSummaryResponse toSummary(Customer customer) {
        return CustomerSummaryResponse.builder()
                .customerId(customer.getId())
                .name(customer.getName())
                .status(customer.getStatus() != null ? customer.getStatus().name() : null)
                .createdAt(customer.getCreatedAt())
                .build();
    }
}