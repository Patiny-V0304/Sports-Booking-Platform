package com.sportsplatform.facility.entity;

import com.sportsplatform.common.enums.SportTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "courts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "court_name", length = 50, nullable = false)
    private String courtName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", length = 20, nullable = false)
    private SportTypeEnum sportType;

    @Builder.Default
    @Column(length = 20, nullable = false)
    private String status = "ACTIVE"; // ACTIVE | MAINTENANCE
}