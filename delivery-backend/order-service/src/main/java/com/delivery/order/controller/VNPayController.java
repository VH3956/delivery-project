package com.delivery.order.controller;

import com.delivery.order.constant.ErrorCode;
import com.delivery.order.entity.Order;
import com.delivery.order.exception.BusinessException;
import com.delivery.order.model.ApiResponse;
import com.delivery.order.repository.OrderRepository;
import com.delivery.order.service.VNPayService;
import com.delivery.order.util.ResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
public class VNPayController {

    private final VNPayService vnPayService;
    private final OrderRepository orderRepository; // Needed to fetch the Order entity

    @GetMapping("/payment-url")
    public ResponseEntity<ApiResponse<String>> createPaymentUrl(@RequestParam String orderId) { // <-- FIX VOID to STRING
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        String paymentUrl = vnPayService.createPaymentUrl(order);
        return ResponseEntity.ok(ResponseUtils.success(paymentUrl));
    }

    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> handleIpn(HttpServletRequest request) {
        // Convert HttpServletRequest parameters to Map<String, String> for VNPayService
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });

        Map<String, String> response = vnPayService.processIpn(params);
        return ResponseEntity.ok(response);
    }
}