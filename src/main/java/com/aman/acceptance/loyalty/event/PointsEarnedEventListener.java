package com.aman.acceptance.loyalty.event;

import com.aman.acceptance.loyalty.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointsEarnedEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPointsEarned(PointsEarnedEvent event) {
        try {
            notificationService.sendPointsEarnedNotification(
                    event.getAccountId(),
                    event.getLoyaltyTransactionId(),
                    event.getEarnedPoints()
            );
        } catch (Exception e) {
            // Notification failures must never affect the (already committed)
            // earning transaction - log and swallow.
            log.error(
                    "Failed to send points-earned notification for loyaltyTransactionId={}",
                    event.getLoyaltyTransactionId(),
                    e
            );
        }
    }
}
