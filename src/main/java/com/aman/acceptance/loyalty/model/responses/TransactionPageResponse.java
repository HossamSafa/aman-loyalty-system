package com.aman.acceptance.loyalty.model.responses;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
@Getter
@RequiredArgsConstructor
public class TransactionPageResponse {
        private final List<TransactionResponse> items;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
}
