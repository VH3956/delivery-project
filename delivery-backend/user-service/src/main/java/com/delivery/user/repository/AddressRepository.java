package com.delivery.user.repository;

import com.delivery.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {

    // Find all addresses belonging to a specific user
    List<Address> findAllByUserId(String userId);

    // Find a specific address by its ID and the User's ID (Security check)
    Optional<Address> findByIdAndUserId(String id, String userId);
}