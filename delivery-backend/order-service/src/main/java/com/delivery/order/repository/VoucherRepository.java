package com.delivery.order.repository;

import com.delivery.order.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {

    // Find a voucher by the text code the user typed, but only if it's currently active!
    Optional<Voucher> findByCodeAndIsActiveTrue(String code);

    boolean existsByCode(String code);
}