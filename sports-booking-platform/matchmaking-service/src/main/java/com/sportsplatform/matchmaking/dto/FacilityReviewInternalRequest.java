package com.sportsplatform.matchmaking.dto;

import java.util.UUID;

public record FacilityReviewInternalRequest(
        UUID bookingId,
        UUID userId,
        int starRating,
        String comment
) {}