package com.sportsplatform.booking.entity;

import com.sportsplatform.common.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_code", length = 30, unique = true, nullable = false)
    private String bookingCode;

    @Column(name = "booker_id", nullable = false)
    private UUID bookerId;

    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "court_id", nullable = false)
    private UUID courtId;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "play_date", nullable = false)
    private LocalDate playDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    @Column(name = "has_matchmaking", nullable = false)
    private Boolean hasMatchmaking = false;

    @Column(name = "court_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal courtPrice;

    @Builder.Default
    @Column(name = "facility_discount", precision = 12, scale = 2, nullable = false)
    private BigDecimal facilityDiscount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "platform_discount", precision = 12, scale = 2, nullable = false)
    private BigDecimal platformDiscount = BigDecimal.ZERO;

    @Column(name = "deposit_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal depositAmount;

    @Column(name = "remaining_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal remainingAmount;

    @Column(name = "platform_fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal platformFee;

    @Column(name = "net_owner_deposit_rev", precision = 12, scale = 2, nullable = false)
    private BigDecimal netOwnerDepositRev;

    @Column(name = "qr_checkin_token", length = 100, unique = true, nullable = false)
    private String qrCheckinToken;

    @Builder.Default
    @Column(name = "reschedule_count", nullable = false)
    private Integer rescheduleCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private BookingStatus status;

    @Column(name = "cancel_reason", length = 30)
    private String cancelReason;

    @Builder.Default
    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}