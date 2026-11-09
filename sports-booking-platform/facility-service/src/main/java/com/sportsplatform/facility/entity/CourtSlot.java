package com.sportsplatform.facility.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "court_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(name = "play_date", nullable = false)
    private LocalDate playDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Builder.Default
    @Column(length = 20, nullable = false)
    private String status = "AVAILABLE"; // AVAILABLE | LOCKED | BOOKED

    @Column(name = "current_booking_id")
    private UUID currentBookingId;
}