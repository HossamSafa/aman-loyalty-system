package com.aman.acceptance.loyalty.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponseDto<T> {
    private boolean success;
    private T data;
}