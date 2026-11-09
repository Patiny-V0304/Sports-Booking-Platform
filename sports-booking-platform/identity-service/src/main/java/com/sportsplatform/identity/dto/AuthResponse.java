package com.sportsplatform.identity.dto;

import com.sportsplatform.common.enums.RoleEnum;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        UUID userId,
        String fullName,
        RoleEnum role,
        Integer reliabilityScore
) {}