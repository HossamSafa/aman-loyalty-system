package com.aman.acceptance.loyalty.model.responses;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
@Getter
@RequiredArgsConstructor
public class NearestExpiryResponse {
    private final Long points;
    private final  Instant expiresAt;
}
