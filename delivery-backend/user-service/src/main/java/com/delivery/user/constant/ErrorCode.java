package com.delivery.user.constant;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // System Errors
    INTERNAL_SERVER_ERROR("server.error", 500),

    // Business Errors
    USER_NOT_FOUND("user.not.found", 404),
    INVALID_PASSWORD("user.invalid.password", 400),
    INVALID_OTP("otp.invalid", 400),

    // Used for our test
    TEST_ERROR("test.error", 400);

    private final String code;
    private final int status;

    ErrorCode(String code, int status) {
        this.code = code;
        this.status = status;
    }
}