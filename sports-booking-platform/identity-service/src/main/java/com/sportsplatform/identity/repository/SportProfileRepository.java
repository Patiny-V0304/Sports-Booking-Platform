package com.sportsplatform.identity.repository;

import com.sportsplatform.common.enums.SportTypeEnum;
import com.sportsplatform.identity.entity.SportProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SportProfileRepository extends JpaRepository<SportProfile, UUID> {
    List<SportProfile> findByUserId(UUID userId);
    Optional<SportProfile> findByUserIdAndSportType(UUID userId, SportTypeEnum sportType);
}