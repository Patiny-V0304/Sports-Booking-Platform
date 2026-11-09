package com.sportsplatform.booking.repository;

import com.sportsplatform.booking.entity.Booking;
import com.sportsplatform.common.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByBookingCode(String bookingCode);
    Optional<Booking> findByQrCheckinToken(String qrCheckinToken);
    List<Booking> findByBookerIdOrderByCreatedAtDesc(UUID bookerId);
    List<Booking> findByFacilityIdAndStatus(UUID facilityId, BookingStatus status);
}