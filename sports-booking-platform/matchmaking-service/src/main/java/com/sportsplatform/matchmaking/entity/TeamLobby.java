package com.sportsplatform.matchmaking.entity;

import com.sportsplatform.common.enums.SkillLevelEnum;
import com.sportsplatform.common.enums.SportTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "team_lobbies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamLobby {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", unique = true, nullable = false)
    private UUID bookingId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", length = 20, nullable = false)
    private SportTypeEnum sportType;

    @Column(name = "play_date", nullable = false)
    private LocalDate playDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "max_recruits", nullable = false)
    private Integer maxRecruits;

    @Builder.Default
    @Column(name = "current_recruits", nullable = false)
    private Integer currentRecruits = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_skill_level", length = 20, nullable = false)
    private SkillLevelEnum requiredSkillLevel;

    @Column(name = "share_amount_per_person", precision = 12, scale = 2, nullable = false)
    private BigDecimal shareAmountPerPerson;

    @Column(name = "host_vietqr_url", columnDefinition = "TEXT", nullable = false)
    private String hostVietqrUrl;

    @Builder.Default
    @Column(length = 20, nullable = false)
    private String status = "OPEN"; // OPEN | FULL | CANCELLED | COMPLETED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}