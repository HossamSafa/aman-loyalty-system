package com.aman.acceptance.loyalty.model.responses;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class EarningResponse {

    private final String loyaltyTransactionId;

    private final String sourceTransactionId;

    private final Long earnedPoints;

    private final String pointsStatus;

    private final OffsetDateTime unlockAt;

    private final OffsetDateTime expiresAt;

    private final Integer appliedRuleVersion;

    private final BalanceResponse balance;

}
