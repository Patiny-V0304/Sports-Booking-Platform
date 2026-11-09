package com.sportsplatform.booking.repository;

import com.sportsplatform.booking.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
}