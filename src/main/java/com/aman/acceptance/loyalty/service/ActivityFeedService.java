package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.AuditEvent;
import com.aman.acceptance.loyalty.model.response.ActivityFeedItemResponse;
import com.aman.acceptance.loyalty.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityFeedService {

    private final AuditEventRepository auditEventRepository;

    public List<ActivityFeedItemResponse> getRecentActivity() {
        return auditEventRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private ActivityFeedItemResponse toResponse(AuditEvent event) {
        return ActivityFeedItemResponse.builder()
                .auditId(event.getId())
                .action(event.getAction())
                .accountId(event.getEntityId())
                .actorId(event.getActorId())
                .occurredAt(event.getCreatedAt())
                .build();
    }
}