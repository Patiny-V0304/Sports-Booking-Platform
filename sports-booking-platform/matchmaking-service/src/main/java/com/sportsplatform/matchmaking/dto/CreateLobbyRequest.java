package com.sportsplatform.matchmaking.dto;

import com.sportsplatform.common.enums.SkillLevelEnum;
import com.sportsplatform.common.enums.SportTypeEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateLobbyRequest(
        @NotNull(message = "Mã đơn đặt sân (bookingId) không được để trống")
        UUID bookingId,
        @NotNull(message = "Môn thể thao không được để trống")
        SportTypeEnum sportType,
        @Min(value = 1, message = "Số lượng cần tuyển tối thiểu là 1")
        int maxRecruits,
        @NotNull(message = "Trình độ yêu cầu không được để trống")
        SkillLevelEnum requiredSkillLevel,
        @NotNull(message = "Số tiền chia sẻ mỗi người không được để trống")
        BigDecimal shareAmountPerPerson,
        @NotBlank(message = "URL mã VietQR của Chủ phòng không được để trống")
        String hostVietqrUrl
) {}