package com.aman.acceptance.loyalty.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class Meta {

    private String correlationId;
    private Instant timestamp;

    public static Meta now(){

        return new Meta("cor-" + UUID.randomUUID().toString().substring(0,8), Instant.now());
    }
}
