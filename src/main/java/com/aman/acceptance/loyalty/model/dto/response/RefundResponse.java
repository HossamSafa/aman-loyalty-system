package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.model.dto.BalanceDto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {

    private String refundTransactionId;

    private String originalTransactionId;

    private String status;

    private Integer canceledLockedPoints;

    private Integer reversedEarnedPoints;

    private Integer restoredRedeemedPoints;

    private MoneyResponseDto restoredRedemptionValue;
///  "value": "0.00"
    private BalanceDto balance;
}
