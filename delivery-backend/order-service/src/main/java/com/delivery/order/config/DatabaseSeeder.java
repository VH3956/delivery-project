package com.delivery.order.config;

import com.delivery.order.entity.Order;
import com.delivery.order.entity.OrderTimeline;
import com.delivery.order.entity.Voucher;
import com.delivery.order.enums.OrderStatus;
import com.delivery.order.repository.OrderRepository;
import com.delivery.order.repository.OrderTimelineRepository;
import com.delivery.order.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final OrderTimelineRepository orderTimelineRepository;
    private final VoucherRepository voucherRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // Seed voucher
        if (!voucherRepository.existsByCode("FREESHIP")) {
            voucherRepository.save(
                    Voucher.builder()
                            .code("FREESHIP")
                            .discountAmount(new BigDecimal("20000.00"))
                            .minOrderValue(new BigDecimal("15000.00"))
                            .isActive(true)
                            .build()
            );

            System.out.println("🎟️ Voucher FREESHIP seeded!");
        }

        // Seed orders
        if (orderRepository.count() == 0) {
            System.out.println("📦 Seeding initial orders...");

            // --- Order 1: A brand-new order waiting for a shipper ---
            Order order1 = Order.builder()
                    .customerId("mock-customer-id-001")
                    .pickupAddressId("address-id-A")
                    .deliveryAddressId("address-id-B")
                    .itemName("MacBook Pro M3")
                    .itemWeight(new BigDecimal("2.5"))
                    .note("Fragile, please handle with care")
                    .distanceKm(new BigDecimal("5.2"))
                    .deliveryFee(new BigDecimal("25000"))
                    .codAmount(new BigDecimal("35000000"))
                    .totalAmount(new BigDecimal("35025000"))
                    .status(OrderStatus.CREATED)
                    .build();

            Order savedOrder1 = orderRepository.save(order1);

            orderTimelineRepository.save(
                    OrderTimeline.builder()
                            .order(savedOrder1)
                            .status(OrderStatus.CREATED)
                            .description("Customer created the order. Waiting for driver assignment.")
                            .build()
            );

            // --- Order 2: An order currently in transit ---
            Order order2 = Order.builder()
                    .customerId("mock-customer-id-002")
                    .shipperId("mock-shipper-id-999")
                    .pickupAddressId("address-id-C")
                    .deliveryAddressId("address-id-D")
                    .itemName("Office Documents")
                    .itemWeight(new BigDecimal("0.5"))
                    .distanceKm(new BigDecimal("12.4"))
                    .deliveryFee(new BigDecimal("45000"))
                    .codAmount(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("45000"))
                    .status(OrderStatus.IN_TRANSIT)
                    .build();

            Order savedOrder2 = orderRepository.save(order2);

            orderTimelineRepository.save(
                    OrderTimeline.builder()
                            .order(savedOrder2)
                            .status(OrderStatus.CREATED)
                            .description("Customer created the order.")
                            .build()
            );

            orderTimelineRepository.save(
                    OrderTimeline.builder()
                            .order(savedOrder2)
                            .status(OrderStatus.ASSIGNED)
                            .description("Driver has accepted the order and is on the way to pickup.")
                            .build()
            );

            orderTimelineRepository.save(
                    OrderTimeline.builder()
                            .order(savedOrder2)
                            .status(OrderStatus.IN_TRANSIT)
                            .description("Driver picked up the package. Heading to destination.")
                            .build()
            );

            System.out.println("✅ Order seeding complete!");
        }
    }
}