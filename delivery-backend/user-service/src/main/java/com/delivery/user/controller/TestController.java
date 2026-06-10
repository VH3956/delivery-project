package com.delivery.user.controller;

import com.delivery.user.constant.ErrorCode;
import com.delivery.user.exception.BusinessException;
import com.delivery.user.model.ApiResponse;
import com.delivery.user.util.ResponseUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // Test 1: Simulating a successful response (Tests Phase 1)
    @GetMapping("/success")
    public ResponseEntity<ApiResponse<Map<String, String>>> testSuccess() {
        Map<String, String> data = Map.of("projectName", "Enterprise Architecture", "status", "Awesome");
        return ResponseEntity.ok(ResponseUtils.success(data));
    }

    // Test 2: Simulating a business failure (Tests Phase 2)
    @GetMapping("/error")
    public ResponseEntity<ApiResponse<Void>> testError() {
        // We throw our custom exception. The @RestControllerAdvice will catch it automatically!
        throw new BusinessException(ErrorCode.TEST_ERROR);
    }

    // Test 3: Guarding with Role-Based Checks (Tests Phase 3 Security)
    @GetMapping("/admin-only")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> testAdminGuard() {
        return ResponseEntity.ok(ResponseUtils.success("Welcome, Administrator! Your role verification passed."));
    }
}