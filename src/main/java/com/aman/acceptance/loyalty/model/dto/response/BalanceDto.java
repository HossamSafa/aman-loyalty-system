package com.aman.acceptance.loyalty.model.dto.response;

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