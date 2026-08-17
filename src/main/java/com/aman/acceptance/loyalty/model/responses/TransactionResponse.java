package com.aman.acceptance.loyalty.model.responses;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
@Getter
@RequiredArgsConstructor
public class TransactionResponse{
    private final String loyaltyTransactionId;
    private final String type;
    private final  Long points;
    private final String status;
    private final String sourceTransactionId;
    private final Instant createdAt;
}
