package com.sportsplatform.facility.repository;

import com.sportsplatform.facility.entity.CourtSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CourtSlotRepository extends JpaRepository<CourtSlot, UUID> {
    List<CourtSlot> findByCourt_Facility_IdAndPlayDateOrderByStartTimeAsc(UUID facilityId, LocalDate playDate);
}