package com.aman.acceptance.loyalty.model.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RiskSummaryResponse {
    private long accountsFrozenLast30Days;
    private long adjustmentsLast30Days;
}