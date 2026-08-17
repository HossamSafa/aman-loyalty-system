package com.aman.acceptance.loyalty.model.responses;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
@Getter
@RequiredArgsConstructor
public class MetaResponse{
    private final String correlationId;
    private final Instant timestamp;
    }


