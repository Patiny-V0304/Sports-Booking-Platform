package com.sportsplatform.facility.repository;

import com.sportsplatform.facility.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourtRepository extends JpaRepository<Court, UUID> {
    List<Court> findByFacilityId(UUID facilityId);
}