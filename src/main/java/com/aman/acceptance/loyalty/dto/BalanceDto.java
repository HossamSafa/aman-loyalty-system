package com.aman.acceptance.loyalty.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceDto {

    private Integer available;

    private Integer locked;

    private Integer reserved;

    private Integer totalOwned;
}
