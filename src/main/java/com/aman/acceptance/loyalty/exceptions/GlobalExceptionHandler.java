package com.aman.acceptance.loyalty.exceptions;


import com.aman.acceptance.loyalty.model.responses.ApiResponse;
import com.aman.acceptance.loyalty.model.responses.MetaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleNotFound(
            ResourceNotFoundException exception
    ) {

        Map<String, Object> error = Map.of(
                "code", "LOYALTY_ACCOUNT_NOT_FOUND",
                "message", exception.getMessage(),
                "retryable", false
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        new ApiResponse<>(
                                false,
                                error,
                                new MetaResponse(
                                        null,
                                        Instant.now()
                                )
                        )
                );
    }
}
