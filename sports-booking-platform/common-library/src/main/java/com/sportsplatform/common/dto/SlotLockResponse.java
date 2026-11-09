package com.sportsplatform.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SlotLockResponse(
        UUID slotId,
        UUID courtId,
        UUID facilityId,
        String facilityName,
        String courtName,
        boolean isOutdoor,
        LocalDate playDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price
) {}