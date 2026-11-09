package com.sportsplatform.facility.dto;

import com.sportsplatform.common.enums.SportTypeEnum;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SlotViewResponse(
        UUID slotId,
        UUID courtId,
        String courtName,
        SportTypeEnum sportType,
        LocalDate playDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price,
        String status // AVAILABLE | LOCKED | BOOKED
) {}