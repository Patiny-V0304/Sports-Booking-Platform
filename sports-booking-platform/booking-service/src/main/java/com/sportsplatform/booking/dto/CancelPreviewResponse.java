package com.sportsplatform.booking.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CancelPreviewResponse(
        UUID bookingId,
        String bookingCode,
        double hoursUntilPlay,
        String appliedPolicyRule,
        int refundPercentage,
        BigDecimal depositPaid,
        BigDecimal estimatedRefundAmount,
        boolean canRescheduleInstead
) {}