package com.sportsplatform.matchmaking.dto;

import com.sportsplatform.common.enums.SkillLevelEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PostMatchReviewRequest(
        @NotNull UUID bookingId,
        UUID lobbyId,
        FacilityReviewItem facilityReview,
        List<PlayerReviewItem> anonymousPlayerReviews
) {
    public record FacilityReviewItem(
            @Min(1) @Max(5) int starRating,
            String comment
    ) {}

    public record PlayerReviewItem(
            @NotNull UUID targetUserId,
            @NotNull SkillLevelEnum votedSkillLevel,
            boolean isNoShow
    ) {}
}