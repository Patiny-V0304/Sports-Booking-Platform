package com.sportsplatform.identity.dto;

import com.sportsplatform.common.enums.SkillLevelEnum;
import com.sportsplatform.common.enums.SportTypeEnum;
import jakarta.validation.constraints.NotNull;

public record UpdateSportProfileRequest(
        @NotNull(message = "Môn thể thao không được để trống")
        SportTypeEnum sportType,

        @NotNull(message = "Mức trình độ không được để trống")
        SkillLevelEnum skillLevel
) {}