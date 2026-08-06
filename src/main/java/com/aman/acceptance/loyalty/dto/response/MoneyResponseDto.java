package com.aman.acceptance.loyalty.dto.response;

import com.aman.acceptance.loyalty.enums.CurrencyCode;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyResponseDto {

    private BigDecimal value;

    private CurrencyCode currency;
}



/// "value": "0.00" according to document    and money dto contain @positive