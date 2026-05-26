package com.delivery.user.repository;

import com.delivery.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}