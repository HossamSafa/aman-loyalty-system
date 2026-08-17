package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.model.dto.common.MetaDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoyaltyException.class)
    public ResponseEntity<Map<String, Object>> handleLoyaltyException(LoyaltyException ex) {

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", ex.getCode().name());
        errorBody.put("message", ex.getMessage());
        errorBody.put("retryable", ex.isRetryable());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorBody);
        response.put("meta", MetaDto.builder()
                .correlationId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build());

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", "LOYALTY_INVALID_MOBILE");
        errorBody.put("message", errorMessage);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorBody);
        response.put("meta", MetaDto.builder()
                .correlationId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

}