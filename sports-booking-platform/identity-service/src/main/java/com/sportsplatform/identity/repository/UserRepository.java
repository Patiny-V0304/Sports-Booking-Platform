package com.sportsplatform.identity.repository;

import com.sportsplatform.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    Optional<User> findByPhoneOrEmail(String phone, String email);
}