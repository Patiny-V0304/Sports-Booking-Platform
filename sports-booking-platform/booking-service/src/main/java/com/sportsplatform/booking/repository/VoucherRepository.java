package com.sportsplatform.booking.repository;

import com.sportsplatform.booking.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoucherRepository extends JpaRepository<Voucher, UUID> {
    Optional<Voucher> findByCodeIgnoreCase(String code);
}