package com.aman.acceptance.loyalty.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AccountStatusResponse {

    private Long accountId;
    private String status;
    private LocalDateTime changedAt;
    private Long auditId;
}
