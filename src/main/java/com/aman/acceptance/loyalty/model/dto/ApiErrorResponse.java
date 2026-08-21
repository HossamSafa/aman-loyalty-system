package com.aman.acceptance.loyalty.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    private boolean success;

    private ErrorDetail error;

    private Meta meta;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {

        private String code;
        private String message;
        private boolean retryable;
        private Object details;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {

        private String correlationId;
        private LocalDateTime timestamp;
    }
}