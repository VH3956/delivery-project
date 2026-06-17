package com.delivery.dispatch.exception;

import com.delivery.dispatch.constant.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.args = args;
    }
}