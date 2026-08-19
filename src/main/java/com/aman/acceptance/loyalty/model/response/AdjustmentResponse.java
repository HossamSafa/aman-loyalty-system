package com.aman.acceptance.loyalty.model.response;

import com.aman.acceptance.loyalty.model.dto.BalanceDto;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonPropertyOrder({"adjustmentId", "loyaltyTransactionId", "type", "points", "balance", "auditId"})
public class AdjustmentResponse {

    private Long adjustmentId;
    private Long loyaltyTransactionId;
    private String type;
    private Integer points;
    private BalanceDto balance;
    private Long auditId;

}
