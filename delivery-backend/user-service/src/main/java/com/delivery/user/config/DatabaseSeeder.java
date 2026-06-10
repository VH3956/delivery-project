package com.delivery.user.config;

import com.delivery.user.entity.ShipperProfile;
import com.delivery.user.entity.User;
import com.delivery.user.repository.ShipperProfileRepository;
import com.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShipperProfileRepository shipperProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only seed if the database is completely empty
        if (userRepository.count() == 0) {
            System.out.println("🌱 Seeding initial database records...");

            // 1. Create an Admin
            User admin = User.builder()
                    .phone("0999999999")
                    .email("admin@delivery.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .fullName("System Admin")
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .isVerified(true) // Admin is already verified
                    .build();
            userRepository.save(admin);

            // 2. Create a normal Customer
            User customer = User.builder()
                    .phone("0111111111")
                    .email("customer@delivery.com")
                    .passwordHash(passwordEncoder.encode("customer123"))
                    .fullName("Regular Customer")
                    .role(User.Role.CUSTOMER)
                    .isActive(true)
                    .isVerified(true)
                    .build();
            userRepository.save(customer);

            // 3. Create an Approved Shipper
            User shipperUser = User.builder()
                    .phone("0888888888")
                    .email("shipper@delivery.com")
                    .passwordHash(passwordEncoder.encode("shipper123"))
                    .fullName("Fast Shipper")
                    .role(User.Role.SHIPPER)
                    .isActive(true)
                    .isVerified(true)
                    .build();
            userRepository.save(shipperUser);

            ShipperProfile shipperProfile = ShipperProfile.builder()
                    .user(shipperUser)
                    .identityCardNumber("001089123456")
                    .drivingLicense("A1-987654321")
                    .vehiclePlate("29A1-123.45")
                    .isApproved(false) // Admin has already approved them
                    .isOnline(false)   // Ready to take orders
                    .build();
            shipperProfileRepository.save(shipperProfile);

            System.out.println("✅ Database seeding complete!");
        } else {
            System.out.println("⏭️ Database already contains data. Skipping seed.");
        }
    }
}