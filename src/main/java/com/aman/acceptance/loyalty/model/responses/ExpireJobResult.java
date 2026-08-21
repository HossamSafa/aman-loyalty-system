package com.aman.acceptance.loyalty.model.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExpireJobResult {

    private final String jobName;
    private final String status;
    private final int batches;
    private final int processedLots;
    private final int expiredPoints;
    private final int failedLots;
    private final String nextCheckpoint;
    private final long durationMs;

}
