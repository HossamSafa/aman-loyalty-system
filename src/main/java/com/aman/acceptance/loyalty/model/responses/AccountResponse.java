package com.aman.acceptance.loyalty.model.responses;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AccountResponse {
    private final String customerId;
    private final String accountId;
    private final String programId;
    private final String accountStatus;
    private final String mobileNumberMasked;
    private final BalanceResponse balance;
    private final NearestExpiryResponse nearestExpiry;
    private final ConversionResponse conversion;
}
