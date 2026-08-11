package com.aman.acceptance.loyalty.model.responses;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
@Getter
@RequiredArgsConstructor
public class   ConversionResponse{
      private final BigDecimal pointsPerEgp;
      private final BigDecimal egpPerPoint;

}
