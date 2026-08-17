package com.aman.acceptance.loyalty.model.dto.response;

import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ConversionDto {
    private String pointsPerEgp;

    private String egpPerPoint;
}
