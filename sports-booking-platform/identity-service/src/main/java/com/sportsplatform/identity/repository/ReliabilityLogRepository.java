package com.sportsplatform.identity.repository;

import com.sportsplatform.identity.entity.ReliabilityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReliabilityLogRepository extends JpaRepository<ReliabilityLog, UUID> {
    List<ReliabilityLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}