package com.sportsplatform.facility.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RegisterFacilityRequest(
        @NotBlank(message = "Tên cụm sân không được để trống")
        String name,
        @NotBlank(message = "Địa chỉ không được để trống")
        String address,
        @NotNull(message = "Vĩ độ (latitude) không được để trống")
        BigDecimal latitude,
        @NotNull(message = "Kinh độ (longitude) không được để trống")
        BigDecimal longitude,
        @NotBlank(message = "URL giấy phép kinh doanh không được để trống")
        String businessLicenseUrl,
        boolean isOutdoor
) {}