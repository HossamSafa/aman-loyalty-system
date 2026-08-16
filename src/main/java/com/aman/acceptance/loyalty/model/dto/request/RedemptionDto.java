package com.aman.acceptance.loyalty.model.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RedemptionDto {
    private Integer points;

    private BigDecimal discountAmount;

    private Integer minimumPoints;

}
