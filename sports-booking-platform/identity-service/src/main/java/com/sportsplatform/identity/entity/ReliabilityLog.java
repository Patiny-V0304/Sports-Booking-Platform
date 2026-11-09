package com.sportsplatform.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reliability_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReliabilityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount; // VD: -20 (No-show), +2 (Hoàn thành trận)

    @Column(length = 50, nullable = false)
    private String reason; // NO_SHOW | LATE_CANCEL | COMPLETED

    @Column(name = "reference_booking_id")
    private UUID referenceBookingId; // Khóa ngoại logic tham chiếu sang Booking Service

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}