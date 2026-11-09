package com.sportsplatform.facility.repository;

import com.sportsplatform.facility.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FacilityRepository extends JpaRepository<Facility, UUID> {

    List<Facility> findByStatus(String status);

    /**
     * Tìm cụm sân ACTIVE trong bán kính (km) bằng công thức mặt cầu Haversine (6371 km = bán kính Trái Đất)
     */
    @Query(value = """
            SELECT * FROM facilities f
            WHERE f.status = 'ACTIVE'
            AND (6371 * acos(
                cos(radians(:lat)) * cos(radians(CAST(f.latitude AS DOUBLE PRECISION))) *
                cos(radians(CAST(f.longitude AS DOUBLE PRECISION)) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(CAST(f.latitude AS DOUBLE PRECISION)))
            )) <= :radiusKm
            """, nativeQuery = true)
    List<Facility> findNearbyActiveFacilities(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm);
}