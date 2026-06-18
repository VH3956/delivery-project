package com.delivery.user.config;

import com.delivery.user.entity.Role;
import com.delivery.user.entity.ShipperProfile;
import com.delivery.user.entity.User;
import com.delivery.user.entity.UserRole;
import com.delivery.user.repository.RoleRepository;
import com.delivery.user.repository.ShipperProfileRepository;
import com.delivery.user.repository.UserRepository;
import com.delivery.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ShipperProfileRepository shipperProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Core Structural Phase: Seed enterprise database roles if absent
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").description("System Admin Authorities").build()));

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CUSTOMER").description("Standard Customer Access").build()));

        Role shipperRole = roleRepository.findByName("ROLE_SHIPPER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_SHIPPER").description("Logistics Delivery Shipper Access").build()));

        // 2. Data Phase: Seed initial database users if empty
        if (userRepository.count() == 0) {
            System.out.println("🌱 Seeding initial database records with role mappings...");

            // ==========================================
            // 1. Create the Admin User
            // ==========================================
            User admin = User.builder()
                    .phone("0999999999")
                    .email("admin@delivery.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .fullName("System Admin")
                    .isActive(true)
                    .isVerified(true)
                    .build();
            User savedAdmin = userRepository.save(admin);
            userRoleRepository.save(UserRole.builder().user(savedAdmin).role(adminRole).build());

            // 🌟 THE FIX: Attach a "System Support" Shipper Profile to the Admin!
            // Now, if the Admin accepts an order, the frontend won't crash.
            ShipperProfile adminShipperProfile = ShipperProfile.builder()
                    .user(savedAdmin)
                    .identityCardNumber("SYS-ADMIN-001")
                    .drivingLicense("SYSTEM-SUPPORT")
                    .vehiclePlate("HỖ TRỢ CSKH") // Frontend displays this!
                    .rating(new BigDecimal("5.0"))
                    .isApproved(true) // Must be true to bypass approval checks
                    .isOnline(true)   // Always ready
                    .build();
            shipperProfileRepository.save(adminShipperProfile);


            // ==========================================
            // 2. Create a Customer User
            // ==========================================
            User customer = User.builder()
                    .phone("0111111111")
                    .email("customer@delivery.com")
                    .passwordHash(passwordEncoder.encode("customer123"))
                    .fullName("Regular Customer")
                    .isActive(true)
                    .isVerified(true)
                    .build();
            User savedCustomer = userRepository.save(customer);
            userRoleRepository.save(UserRole.builder().user(savedCustomer).role(customerRole).build());


            // ==========================================
            // 3. Create a Regular Shipper User
            // ==========================================
            User shipperUser = User.builder()
                    .phone("0888888888")
                    .email("shipper@delivery.com")
                    .passwordHash(passwordEncoder.encode("shipper123"))
                    .fullName("Fast Shipper")
                    .isActive(true)
                    .isVerified(true)
                    .build();
            User savedShipperUser = userRepository.save(shipperUser);
            userRoleRepository.save(UserRole.builder().user(savedShipperUser).role(shipperRole).build());

            ShipperProfile shipperProfile = ShipperProfile.builder()
                    .user(savedShipperUser)
                    .identityCardNumber("001089123456")
                    .drivingLicense("A1-987654321")
                    .vehiclePlate("29A1-123.45")
                    .rating(new BigDecimal("4.8"))
                    .isApproved(false) // Set true so it's immediately ready for test cycles
                    .isOnline(false)
                    .build();
            shipperProfileRepository.save(shipperProfile);

            System.out.println("✅ Database role-based configuration seeding complete!");
        } else {
            System.out.println("⏭️ Database already contains user records. Skipping seed core setup.");
        }
    }
}