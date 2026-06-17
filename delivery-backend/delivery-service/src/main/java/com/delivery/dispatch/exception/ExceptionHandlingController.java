package com.delivery.dispatch.exception;

import com.delivery.dispatch.constant.ErrorCode;
import com.delivery.dispatch.model.ApiResponse;
import com.delivery.dispatch.util.MessageCommon;
import com.delivery.dispatch.util.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionHandlingController {

    private final MessageCommon messageCommon;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business Exception: {}", ex.getErrorCode().getCode());
        String localizedMessage = messageCommon.getMessage(ex.getErrorCode().getCode(), ex.getArgs());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ResponseUtils.error(ex.getErrorCode().getStatus(), localizedMessage));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access Denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseUtils.error(HttpStatus.FORBIDDEN.value(), "Access Denied: You do not have the required permissions to access this resource."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled Exception: ", ex);
        String localizedMessage = messageCommon.getMessage(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ResponseUtils.error(ErrorCode.INTERNAL_SERVER_ERROR.getStatus(), localizedMessage));
    }
}