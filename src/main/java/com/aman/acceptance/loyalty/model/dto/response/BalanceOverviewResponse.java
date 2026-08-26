package com.aman.acceptance.loyalty.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BalanceOverviewResponse {
    private Long available;
    private Long locked;
    private Long reserved;
    private Long expiringSoon;
    private Long totalOwned;
}