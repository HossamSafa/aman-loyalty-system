package com.aman.acceptance.loyalty.model.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommitRequest(
        @NotBlank(message = "Authorization Code cannot be blank")
        String authorizationCode
) {}
