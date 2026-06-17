package com.delivery.dispatch.util;

import com.delivery.dispatch.model.ApiResponse;
import org.springframework.http.HttpStatus;

public class ResponseUtils {
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .message("Success")
                .data(data)
                .build();
    }

    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .build();
    }

    public static ApiResponse<Void> error(int status, String message) {
        return ApiResponse.<Void>builder()
                .status(status)
                .message(message)
                .build();
    }
}