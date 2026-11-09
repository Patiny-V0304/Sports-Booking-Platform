package com.sportsplatform.identity.entity;

import com.sportsplatform.common.enums.SkillLevelEnum;
import com.sportsplatform.common.enums.SportTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "sport_profiles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "sport_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", length = 20, nullable = false)
    private SportTypeEnum sportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 20, nullable = false)
    private SkillLevelEnum skillLevel;

    @Builder.Default
    @Column(name = "total_evaluations", nullable = false)
    private Integer totalEvaluations = 0;
}