package com.aman.acceptance.loyalty.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"success", "data", "error", "meta"})
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetails error;
    private final Meta meta;

    private ApiResponse(boolean success, T data, ErrorDetails error){

        this.success = success;
        this.data = data;
        this.error = error;
        this.meta = Meta.now();
    }

    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, boolean retryable){
        return new ApiResponse<>(false, null, new ErrorDetails(code, message, retryable));
    }
}
