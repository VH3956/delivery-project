package com.delivery.order.controller;

import com.delivery.order.service.VNPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders/vnpay")
@RequiredArgsConstructor
public class VNPayController {

    private final VNPayService vnPayService;

    // VNPay sends a GET request with all data as query parameters
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> processIpn(@RequestParam Map<String, String> params) {
        log.info("🔔 Received IPN from VNPay for Order: {}", params.get("vnp_TxnRef"));
        return ResponseEntity.ok(vnPayService.processIpn(params));
    }
}