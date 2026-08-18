package com.aman.acceptance.loyalty.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Placeholder implementation. No real notification channel (SMS/push/email)
 * exists in this project yet, so this only logs. Replace with a real
 * implementation of {@link NotificationService} when one is available -
 * the calling code (the AFTER_COMMIT listener) does not need to change.
 */
@Slf4j
@Service
public class LoggingNotificationService implements NotificationService {

    @Override
    public void sendPointsEarnedNotification(
            Long accountId,
            Long loyaltyTransactionId,
            Integer earnedPoints
    ) {
        log.info(
                "Points earned notification: accountId={}, loyaltyTransactionId={}, earnedPoints={}",
                accountId,
                loyaltyTransactionId,
                earnedPoints
        );
    }
}
