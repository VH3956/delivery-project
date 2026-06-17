package com.delivery.dispatch.constant;

import lombok.Getter;

@Getter
public enum ErrorCode {
    
    // System Errors
    INTERNAL_SERVER_ERROR("server.error", 500),
    ACTION_NOT_ALLOWED("action.not.allowed", 400),
    
    // Delivery Domain Errors
    LOCATION_UPDATE_FAILED("location.update.failed", 400),
    DRIVER_NOT_FOUND("driver.not.found", 404);

    private final String code;
    private final int status;

    ErrorCode(String code, int status) {
        this.code = code;
        this.status = status;
    }
}