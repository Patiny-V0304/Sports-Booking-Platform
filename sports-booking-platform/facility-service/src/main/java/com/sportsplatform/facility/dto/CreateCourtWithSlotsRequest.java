package com.sportsplatform.facility.dto;

import com.sportsplatform.common.enums.SportTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateCourtWithSlotsRequest(
        @NotBlank(message = "Tên sân con không được để trống")
        String courtName,
        @NotNull(message = "Môn thể thao không được để trống")
        SportTypeEnum sportType,
        @NotEmpty(message = "Danh sách khung giờ không được để trống")
        List<SlotItem> slots
) {
    public record SlotItem(
            LocalDate playDate,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal price
    ) {}
}