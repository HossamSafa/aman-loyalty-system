package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.exception.AccountException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoyaltyPointsJobScheduler {

    private final LoyaltyPointsExpirationJobService expirationJobService;
    private final LoyaltyPointsUnlockJobService unlockJobService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Africa/Cairo")
    public void processUnlocks() throws AccountException {
        log.info("START UNLOCK JOB");
        unlockJobService.processUnlockJob(LocalDateTime.now(ZoneId.of("Africa/Cairo")), 1000);
        log.info("END OF UNLOCK JOB");

    }

    @Scheduled(cron = "0 1 0 * * *", zone = "Africa/Cairo")    public void processExpirations() throws AccountException {
        log.info("START Expire JOB");
        expirationJobService.processExpireJob(LocalDateTime.now(ZoneId.of("Africa/Cairo")), 1000, 0L);
        log.info("END OF Expire JOB");

    }

}