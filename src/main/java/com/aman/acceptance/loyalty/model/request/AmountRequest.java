package com.aman.acceptance.loyalty.model.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AmountRequest {

    private BigDecimal value;

    private String currency;


}
