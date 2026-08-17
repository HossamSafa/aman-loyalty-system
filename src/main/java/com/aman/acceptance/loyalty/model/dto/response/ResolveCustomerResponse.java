package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.enums.AccountStatus;
import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ResolveCustomerResponse {

    private Long customerId;

    private Long accountId;

    private Long programId;

    private  Boolean newlyEnrolled;

    private AccountStatus accountStatus;

    private String mobileNumberMasked;

    private BalanceDto balance;

    private ConversionDto conversion;

}
