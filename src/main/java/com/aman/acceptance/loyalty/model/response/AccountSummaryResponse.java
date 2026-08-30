package com.aman.acceptance.loyalty.model.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class AccountSummaryResponse {
    private Long accountId;
    private String status;
    private Integer availablePoints;
    private Integer lockedPoints;
    private Integer reservedPoints;
    private List<ActivityFeedItemResponse> recentActivity;
}