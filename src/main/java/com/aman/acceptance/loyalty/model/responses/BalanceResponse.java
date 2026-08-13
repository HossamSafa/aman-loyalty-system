package com.aman.acceptance.loyalty.model.responses;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BalanceResponse {
    private final Integer available;
    private final Integer locked;
    private final Integer reserved;
    private final Integer totalOwned;

}


