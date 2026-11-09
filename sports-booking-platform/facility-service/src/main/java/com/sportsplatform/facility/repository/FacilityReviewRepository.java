package com.sportsplatform.facility.repository;

import com.sportsplatform.facility.entity.FacilityReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FacilityReviewRepository extends JpaRepository<FacilityReview, UUID> {
    boolean existsByBookingId(UUID bookingId);

    @Query("SELECT COALESCE(AVG(r.starRating), 0) FROM FacilityReview r WHERE r.facility.id = :facilityId")
    Double calculateAverageRating(@Param("facilityId") UUID facilityId);
}