package com.delivery.order.constant;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // System Errors
    INTERNAL_SERVER_ERROR("server.error", 500),
    ACTION_NOT_ALLOWED("action.not.allowed", 400),

    // Order Domain Errors
    ORDER_NOT_FOUND("order.not.found", 404),
    INVALID_ORDER_STATUS("order.invalid.status", 400),
    VOUCHER_NOT_FOUND("voucher.not.found", 404),
    VOUCHER_INVALID("voucher.invalid", 400);

    private final String code;
    private final int status;

    ErrorCode(String code, int status) {
        this.code = code;
        this.status = status;
    }
}