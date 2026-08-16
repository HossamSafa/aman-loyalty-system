package com.aman.acceptance.loyalty.model.dto.request;

import com.aman.acceptance.loyalty.enums.RoundingMode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EarningDto
{
    private BigDecimal spendAmount;

    private Integer points;

    private RoundingMode roundingMode;

}
