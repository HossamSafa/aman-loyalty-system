package com.aman.acceptance.loyalty.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LedgerEntryResponse {
    private String loyaltyTransactionId;
    private Long accountId;
    private String mobileNumberMasked;
    private String type;
    private Integer points;
    private String status;
    private String sourceTransactionId;
    private LocalDateTime createdAt;
}