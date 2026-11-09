package com.sportsplatform.common.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingEventPayload(
        String eventType, // BOOKING_CONFIRMED | BOOKING_CANCELLED | MATCH_COMPLETED
        UUID bookingId,
        String bookingCode,
        UUID bookerId,
        BigDecimal refundAmount,
        String message
) {}