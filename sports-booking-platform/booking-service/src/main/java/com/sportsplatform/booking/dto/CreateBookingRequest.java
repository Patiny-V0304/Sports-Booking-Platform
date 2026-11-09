package com.sportsplatform.booking.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull(message = "Mã khung giờ (slotId) không được để trống")
        UUID slotId,
        boolean hasMatchmaking,
        String voucherCode,
        String paymentGateway // VNPAY | MOMO
) {}