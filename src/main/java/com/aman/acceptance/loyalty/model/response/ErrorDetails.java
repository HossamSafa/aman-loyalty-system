package com.aman.acceptance.loyalty.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorDetails {

    private String code;
    private String message;
    private boolean retryable;
}
