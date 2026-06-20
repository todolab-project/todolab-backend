package com.todolab.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String status,
        T data,
        ErrorBody error,
        LocalDateTime timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>("fail", null, new ErrorBody(errorCode.getCode(), errorCode.getMessage()), LocalDateTime.now());
    }

    public record ErrorBody(int code, String message) {}
}
