package com.aman.acceptance.loyalty.exception;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    protected BusinessException(
            ErrorCode code,
            String message,
            HttpStatus status
    ) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static BusinessException notFound(
            ErrorCode code,
            String message
    ) {
        return new BusinessException(
                code,
                message,
                HttpStatus.NOT_FOUND
        );
    }

    public static BusinessException badRequest(
            ErrorCode code,
            String message
    ) {
        return new BusinessException(
                code,
                message,
                HttpStatus.BAD_REQUEST
        );
    }

    public static BusinessException invalid(
            ErrorCode code,
            String message
    ) {
        return new BusinessException(
                code,
                message,
                HttpStatus.UNPROCESSABLE_ENTITY
        );
    }

    public static BusinessException conflict(
            ErrorCode code,
            String message
    ) {
        return new BusinessException(
                code,
                message,
                HttpStatus.CONFLICT
        );
    }

    public static BusinessException internal(
            ErrorCode code,
            String message
    ) {
        return new BusinessException(
                code,
                message,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}




///Using IllegalArgumentException and IllegalStateException may result in an HTTP 500 response, even though the issue is a business validation error rather than a system error.