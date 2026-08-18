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
public class ApiSuccessResponse<T> {

    private boolean success;

    private T data;

    private Meta meta;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {

        private String correlationId;
        private LocalDateTime timestamp;
    }

    public static <T> ApiSuccessResponse<T> of(T data, String correlationId) {
        return ApiSuccessResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(
                        Meta.builder()
                                .correlationId(correlationId)
                                .timestamp(LocalDateTime.now())
                                .build()
                )
                .build();
    }
}
