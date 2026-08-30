package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.model.dto.common.MetaDto;
import com.aman.acceptance.loyalty.model.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import com.aman.acceptance.loyalty.model.dto.common.MetaDto;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;
import com.aman.acceptance.loyalty.model.dto.common.MetaDto;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;


@Slf4j
@RestControllerAdvice

public class GlobalExceptionHandler {

    @ExceptionHandler(LoyaltyException.class)
    public ResponseEntity<Map<String, Object>> handleLoyaltyException(LoyaltyException ex) {
        log.warn("Business error [{}]: {}", ex.getCode(), ex.getMessage());

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
        log.warn("Validation error: {}", ex.getMessage());

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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Route not found: {}", ex.getMessage());
        ApiResponse<Void> body = ApiResponse.error("LOYALTY_ROUTE_NOT_FOUND", "The requested endpoint does not exist.", false);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidSort(InvalidDataAccessApiUsageException ex) {
        log.warn("Invalid query usage: {}", ex.getMessage());
        ApiResponse<Void> body = ApiResponse.error("LOYALTY_VALIDATION_ERROR", "Invalid sort field provided.", false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        ApiResponse<Void> body = ApiResponse.error("LOYALTY_ILLEGAL_STATE", ex.getMessage(), false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ApiResponse<Void> body = ApiResponse.error(
                "LOYALTY_INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.",
                true
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex
    ) {

      log.warn("Access denied: {}", ex.getMessage());

        ApiResponse<Void> body = ApiResponse.error(
                "LOYALTY_ACCESS_DENIED",
                "You do not have permission to perform this operation.",
                false
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(body);
    }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex
    ) {

       log.warn(
               "Business error [{}]: {}",
                ex.getCode(),
               ex.getMessage()
      );

        ApiResponse<Void> body = ApiResponse.error(
                ex.getCode().name(),
                ex.getMessage(),
                false
        );

        return ResponseEntity
                .status(ex.getStatus())
                .body(body);
    }
}