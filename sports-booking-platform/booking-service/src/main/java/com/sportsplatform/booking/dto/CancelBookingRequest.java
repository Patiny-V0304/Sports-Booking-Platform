package com.sportsplatform.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelBookingRequest(
        @NotBlank(message = "Lý do hủy không được để trống")
        String cancelReason // USER_CANCEL | WEATHER_CANCEL | INSUFFICIENT_PLAYERS_4H
) {}