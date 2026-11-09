package com.sportsplatform.matchmaking.dto;

import com.sportsplatform.common.enums.BookingStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingInfoDto(
        UUID id,
        String bookingCode,
        UUID bookerId,
        UUID facilityId,
        UUID courtId,
        UUID slotId,
        LocalDate playDate,
        LocalTime startTime,
        LocalTime endTime,
        Boolean hasMatchmaking,
        BookingStatus status
) {}