package com.delivery.order.service;

import com.delivery.order.config.VNPayConfig;
import com.delivery.order.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.delivery.order.entity.OrderTimeline;
import com.delivery.order.enums.OrderStatus;
import com.delivery.order.repository.OrderRepository;
import com.delivery.order.repository.OrderTimelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final OrderRepository orderRepository;
    private final OrderTimelineRepository orderTimelineRepository;

    @Value("${vnpay.tmn-code:DUMMY_TMN}")
    private String vnpTmnCode;

    @Value("${vnpay.secret-key:DUMMY_SECRET}")
    private String vnpHashSecret;

    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url:http://localhost:5500/success.html}")
    private String vnpReturnUrl;

    public String createPaymentUrl(Order order) {
        // VNPay requires the amount to be multiplied by 100 (e.g., 10000 VND -> 1000000)
        long amount = order.getTotalAmount().longValue() * 100;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnpTmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", order.getId()); // Use your actual Order ID
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang: " + order.getId());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpReturnUrl);
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");

        // Use Asia/Ho_Chi_Minh to ensure it perfectly matches VNPay's servers
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Sort the parameters alphabetically (Required by VNPay)
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Build Hash Data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    // Build Query string
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error building VNPay URL", e);
        }

        // Add the secure cryptographic signature!
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnpHashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnpPayUrl + "?" + queryUrl;
    }

    public Map<String, String> processIpn(Map<String, String> params) {
        Map<String, String> response = new HashMap<>();
        try {
            // 1. Remove hash to recalculate it
            String vnp_SecureHash = params.get("vnp_SecureHash");
            params.remove("vnp_SecureHash");
            params.remove("vnp_SecureHashType");

            // 2. Sort parameters and build hash string
            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            // 3. Verify Signature
            String signValue = VNPayConfig.hmacSHA512(vnpHashSecret, hashData.toString());
            if (!signValue.equals(vnp_SecureHash)) {
                log.error("❌ VNPay IPN Signature verification failed!");
                response.put("RspCode", "97");
                response.put("Message", "Invalid signature");
                return response;
            }

            // 4. Find the order
            String orderId = params.get("vnp_TxnRef");
            Order order = orderRepository.findById(orderId).orElse(null);

            if (order == null) {
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return response;
            }

            // 5. Update Order Status if Payment was successful (00)
            String responseCode = params.get("vnp_ResponseCode");
            if ("00".equals(responseCode)) {
                
                // Only update if it hasn't been updated already!
                if (order.getStatus() == OrderStatus.CREATED) {
                    order.setStatus(OrderStatus.PAID);
                    orderRepository.save(order);

                    OrderTimeline timeline = OrderTimeline.builder()
                            .order(order)
                            .status(OrderStatus.PAID)
                            .description("VNPay payment successful.")
                            .build();
                    orderTimelineRepository.save(timeline);

                    log.info("✅ Order {} marked as PAID via VNPay IPN!", orderId);
                }
            } else {
                log.warn("⚠️ VNPay returned payment failure code: {}", responseCode);
            }

            // Return success signal to VNPay
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");

        } catch (Exception e) {
            log.error("❌ Error processing VNPay IPN", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }
        return response;
    }
}