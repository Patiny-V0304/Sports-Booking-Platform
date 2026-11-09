package com.sportsplatform.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateVoucherRequest(
        @NotBlank String code,
        @NotBlank String issuerType, // FACILITY | PLATFORM
        UUID facilityId,
        @Min(1) @Max(100) int discountPercent,
        @NotNull BigDecimal maxDiscountAmount,
        @NotNull LocalDateTime validTo,
        @Min(1) int usageLimit
) {}