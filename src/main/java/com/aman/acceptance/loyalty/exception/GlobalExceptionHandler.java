package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.model.dto.ApiErrorResponse;
import com.aman.acceptance.loyalty.web.CorrelationIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoyaltyException.class)
    public ResponseEntity<ApiErrorResponse> handleLoyaltyException(
            LoyaltyException exception
    ) {

        ApiErrorResponse response = ApiErrorResponse.builder()
                .success(false)
                .error(
                        ApiErrorResponse.ErrorDetail.builder()
                                .code(exception.getCode())
                                .message(exception.getMessage())
                                .retryable(exception.isRetryable())
                                .details(exception.getDetails())
                                .build()
                )
                .meta(buildMeta())
                .build();

        return ResponseEntity
                .status(exception.getStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception
    ) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.put(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                )
        );

        ApiErrorResponse response = ApiErrorResponse.builder()
                .success(false)
                .error(
                        ApiErrorResponse.ErrorDetail.builder()
                                .code("LOYALTY_VALIDATION_ERROR")
                                .message("Request validation failed")
                                .retryable(false)
                                .details(fieldErrors)
                                .build()
                )
                .meta(buildMeta())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {

        Map<String, String> violations = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                violations.put(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                )
        );

        ApiErrorResponse response = ApiErrorResponse.builder()
                .success(false)
                .error(
                        ApiErrorResponse.ErrorDetail.builder()
                                .code("LOYALTY_VALIDATION_ERROR")
                                .message("Request validation failed")
                                .retryable(false)
                                .details(violations)
                                .build()
                )
                .meta(buildMeta())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    private ApiErrorResponse.Meta buildMeta() {
        return ApiErrorResponse.Meta.builder()
                .correlationId(MDC.get(CorrelationIdFilter.MDC_KEY))
                .timestamp(LocalDateTime.now())
                .build();
    }
}
