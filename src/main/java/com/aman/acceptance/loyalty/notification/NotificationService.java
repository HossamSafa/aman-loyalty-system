package com.aman.acceptance.loyalty.notification;

public interface NotificationService {

    void sendPointsEarnedNotification(
            Long accountId,
            Long loyaltyTransactionId,
            Integer earnedPoints
    );
}
