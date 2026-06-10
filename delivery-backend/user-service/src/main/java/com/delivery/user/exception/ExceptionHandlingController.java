package com.delivery.user.exception;

import com.delivery.user.constant.ErrorCode;
import com.delivery.user.model.ApiResponse;
import com.delivery.user.util.MessageCommon;
import com.delivery.user.util.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionHandlingController {

    private final MessageCommon messageCommon;

    // Catches our custom BusinessExceptions
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business Exception Triggered: {}", ex.getErrorCode().getCode());

        // Translate the error code into readable text
        String localizedMessage = messageCommon.getMessage(ex.getErrorCode().getCode(), ex.getArgs());

        // Wrap it in the Phase 1 ApiResponse
        ApiResponse<Void> response = ResponseUtils.error(ex.getErrorCode().getStatus(), localizedMessage);

        return ResponseEntity.status(ex.getErrorCode().getStatus()).body(response);
    }

    // Catches Spring Security Role failures (403 Forbidden)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access Denied: {}", ex.getMessage());
        
        ApiResponse<Void> response = ResponseUtils.error(
                org.springframework.http.HttpStatus.FORBIDDEN.value(), 
                "Access Denied: You do not have the required permissions to access this resource."
        );
        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(response);
    }

    // Catches all other unexpected Java errors (NullPointer, Database crashes, etc.)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled System Exception: ", ex);

        String localizedMessage = messageCommon.getMessage(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        ApiResponse<Void> response = ResponseUtils.error(ErrorCode.INTERNAL_SERVER_ERROR.getStatus(), localizedMessage);

        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus()).body(response);
    }
}