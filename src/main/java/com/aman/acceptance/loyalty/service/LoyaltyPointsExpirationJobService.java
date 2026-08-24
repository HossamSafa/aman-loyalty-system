package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.model.responses.ExpireBatchResult;
import com.aman.acceptance.loyalty.model.responses.ExpireJobResult;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.aman.acceptance.loyalty.exception.AccountException;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class LoyaltyPointsExpirationJobService {

    private final LoyaltyPointsLifecycleService loyaltyPointsLifecycleService;

    public ExpireJobResult processExpireJob(
            final LocalDateTime asOf, final int batchSize, final Long checkpointId) throws AccountException {

        final long startTime = System.currentTimeMillis();

        final Pageable pageable = PageRequest.of(0, batchSize);

        Long currentCheckpointId = checkpointId == null ? 0L : checkpointId;

        int batches = 0;
        int processedLots = 0;
        int expiredPoints = 0;
        int failedLots = 0;

        String nextCheckpoint = null;

        while (true) {

            final ExpireBatchResult result = loyaltyPointsLifecycleService.processExpire(
                            pageable, LotStatus.AVAILABLE, asOf, 0, currentCheckpointId);

            if (result == null) {

                break;

            }

            batches++;

            processedLots += result.getProcessedLots();

            expiredPoints += result.getExpiredPoints();

            nextCheckpoint = result.getNextCheckpoint();

            currentCheckpointId = Long.valueOf(nextCheckpoint.replace("lot-", ""));
        }

        final long durationMs = System.currentTimeMillis() - startTime;

        return new ExpireJobResult("EXPIRE_POINTS", "COMPLETED", batches, processedLots,

                expiredPoints, failedLots, nextCheckpoint, durationMs);

    }
}
