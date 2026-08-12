package com.aman.acceptance.loyalty.model.dto.response;

import java.time.Instant;

public record OtpMetadataDto(
        String mobileNumberMasked,
        Instant expiresAt,
        Integer attemptsRemaining
) {
}
