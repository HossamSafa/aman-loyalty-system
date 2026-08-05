package com.aman.acceptance.loyalty.dto.response;

import com.aman.acceptance.loyalty.dto.BalanceDto;
import com.aman.acceptance.loyalty.dto.MoneyDto;
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

    private MoneyDto restoredRedemptionValue;

    private BalanceDto balance;
}
