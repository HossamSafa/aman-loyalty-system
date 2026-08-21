package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.exception.AccountException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class LoyaltyPointsUnlockJobService {

    private final LoyaltyPointsLifecycleService loyaltyPointsLifecycleService;

    public void processUnlockJob(final LocalDateTime asOf, final int batchSize) throws AccountException {

        final Pageable pageable = PageRequest.of(0, batchSize);

        while (true) {

            final int processedLots = loyaltyPointsLifecycleService.processUnlocks(

                    pageable, LotStatus.LOCKED, asOf, 0);

            if (processedLots == 0) {

                break;

            }
        }
    }
}
