package com.aman.acceptance.loyalty.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AuditEventResponse {
    private Long id;
    private String actorId;
    private String action;
    private String entityType;
    private Long entityId;
    private String afterJson;
    private LocalDateTime createdAt;
}