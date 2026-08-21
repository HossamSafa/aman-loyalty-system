package com.aman.acceptance.loyalty.model.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExpireBatchResult {

    final String jobName;
    private final int processedLots;
    private final int expiredPoints;
    private final String nextCheckpoint;

}
