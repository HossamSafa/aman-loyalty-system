package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.dto.ApiSuccessResponse;
import com.aman.acceptance.loyalty.model.dto.EarningRequest;
import com.aman.acceptance.loyalty.model.dto.EarningResponse;
import com.aman.acceptance.loyalty.service.EarningService;
import com.aman.acceptance.loyalty.web.ClientIdentityResolver;
import com.aman.acceptance.loyalty.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/earnings")
@RequiredArgsConstructor
public class EarningController {

    private final EarningService earningService;
    private final ClientIdentityResolver clientIdentityResolver;

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<EarningResponse>> earn(
            @Valid @RequestBody EarningRequest request,
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "Idempotency-Key must not be blank")
            @Size(max = 255, message = "Idempotency-Key must not exceed 255 characters")
            String idempotencyKey,
            HttpServletRequest httpServletRequest
    ) {
        // BLOCKED: this service has no authentication layer yet, so
        // clientId cannot be safely resolved - see ClientIdentityResolver.
        // This call currently always throws ClientIdentityUnavailableException
        // (HTTP 501) until real JWT validation is wired in.
        String clientId = clientIdentityResolver.resolveClientId(httpServletRequest);

        EarningResponse response =
                earningService.earn(request, idempotencyKey, clientId);

        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(response, correlationId));
    }
}
