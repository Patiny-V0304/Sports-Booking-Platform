package com.sportsplatform.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentWebhookRequest(
        @NotBlank String bookingCode,
        @NotBlank String gatewayTxnRef,
        @NotBlank String responseCode // "00" = Thành công, khác "00" = Thất bại
) {}