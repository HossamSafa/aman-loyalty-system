package com.aman.acceptance.loyalty.model.dto.common;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetaDto {

    private String correlationId;

    private LocalDateTime timestamp;


    public static MetaDto now() {
        return MetaDto.builder()
                .correlationId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build();
    }

}