package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.model.dto.ApiErrorResponse;
import com.aman.acceptance.loyalty.web.CorrelationIdFilter;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import com.aman.acceptance.loyalty.model.dto.common.MetaDto;
import com.aman.acceptance.loyalty.model.response.ApiResponse;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoyaltyException.class)
    public ResponseEntity<ApiErrorResponse> handleLoyaltyException(
            LoyaltyException exception
    ) {

        log.warn("Business error [{}]: {}", exception.getCode(), exception.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .success(false)
                .error(
                        ApiErrorResponse.ErrorDetail.builder()
                                .code(exception.getCode().name())
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

        log.warn("Validation error: {}", exception.getMessage());

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception exception) {

        log.error("Unexpected error occurred", exception);

        ApiErrorResponse response = ApiErrorResponse.builder()
                .success(false)
                .error(
                        ApiErrorResponse.ErrorDetail.builder()
                                .code("LOYALTY_INTERNAL_ERROR")
                                .message("An unexpected error occurred. Please try again later.")
                                .retryable(true)
                                .details(null)
                                .build()
                )
                .meta(buildMeta())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private ApiErrorResponse.Meta buildMeta() {
        return ApiErrorResponse.Meta.builder()
                .correlationId(MDC.get(CorrelationIdFilter.MDC_KEY))
                .timestamp(LocalDateTime.now())
                .build();
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

