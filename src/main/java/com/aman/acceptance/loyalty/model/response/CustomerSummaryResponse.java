package com.aman.acceptance.loyalty.model.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CustomerSummaryResponse {

    private Long customerId;
    private String name;
    private String status;
    private LocalDateTime createdAt;

}
