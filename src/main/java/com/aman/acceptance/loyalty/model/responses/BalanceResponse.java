package com.aman.acceptance.loyalty.model.responses;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BalanceResponse {
    private final Long available;
    private final Long locked;
    private final Long reserved;
    private final Long totalOwned;

}


