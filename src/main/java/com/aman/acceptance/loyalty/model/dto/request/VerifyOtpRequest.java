package com.aman.acceptance.loyalty.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "\\d{6}")
        String otp
) {
}
