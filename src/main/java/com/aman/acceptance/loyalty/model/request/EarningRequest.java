package com.aman.acceptance.loyalty.model.request;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class EarningRequest {

    private  String accountId;

    private String sourceTransactionId;

    private AmountRequest amount;

    private OffsetDateTime transactionTime;

    private String channel;

}
