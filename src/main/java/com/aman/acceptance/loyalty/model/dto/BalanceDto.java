package com.aman.acceptance.loyalty.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceDto {
    private Integer available;
    private Integer locked;
    private Integer reserved;
    private Integer totalOwned;
}