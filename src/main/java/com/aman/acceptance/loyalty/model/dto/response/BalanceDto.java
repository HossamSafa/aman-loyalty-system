package com.aman.acceptance.loyalty.model.dto.response;

public record BalanceDto(
        Integer available,
        Integer locked,
        Integer reserved,
        Integer totalOwned
) {}
