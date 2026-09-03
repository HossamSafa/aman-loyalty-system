package com.aman.acceptance.loyalty.model.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ActivityFeedItemResponse {
    private Long auditId;
    private String action;
    private Long accountId;
    private String actorId;
    private LocalDateTime occurredAt;
}