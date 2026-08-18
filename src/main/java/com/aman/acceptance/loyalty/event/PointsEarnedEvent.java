package com.aman.acceptance.loyalty.event;

import lombok.Getter;

@Getter
public class PointsEarnedEvent {

    private final Long accountId;
    private final Long loyaltyTransactionId;
    private final Integer earnedPoints;

    public PointsEarnedEvent(
            Long accountId,
            Long loyaltyTransactionId,
            Integer earnedPoints
    ) {
        this.accountId = accountId;
        this.loyaltyTransactionId = loyaltyTransactionId;
        this.earnedPoints = earnedPoints;
    }
}
