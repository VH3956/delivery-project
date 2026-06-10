package com.delivery.user.exception;

import com.delivery.user.constant.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getCode()); // Sets the base message to the raw code string
        this.errorCode = errorCode;
        this.args = args; // Allows dynamic variables in messages (e.g., "User {0} not found")
    }
}