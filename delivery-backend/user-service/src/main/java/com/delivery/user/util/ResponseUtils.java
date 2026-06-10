package com.delivery.user.util;

import com.delivery.user.model.ApiResponse;
import org.springframework.http.HttpStatus;

public class ResponseUtils {

    // For successful requests that return data
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .message("Success")
                .data(data)
                .build();
    }

    // For successful requests that just return a message (like "Code Sent")
    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .build();
    }

    // For errors
    public static ApiResponse<Void> error(int status, String message) {
        return ApiResponse.<Void>builder()
                .status(status)
                .message(message)
                .build();
    }
}