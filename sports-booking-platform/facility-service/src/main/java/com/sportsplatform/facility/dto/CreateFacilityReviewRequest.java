package com.sportsplatform.facility.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateFacilityReviewRequest(
        @NotNull UUID bookingId,
        @NotNull UUID userId,
        @Min(1) @Max(5) int starRating,
        String comment
) {}