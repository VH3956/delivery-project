package com.delivery.order.config;

import com.delivery.order.entity.Voucher;
import com.delivery.order.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

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
  
    }
}