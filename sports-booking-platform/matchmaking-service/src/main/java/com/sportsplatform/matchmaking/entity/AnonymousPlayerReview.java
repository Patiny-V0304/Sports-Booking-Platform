package com.sportsplatform.matchmaking.entity;

import com.sportsplatform.common.enums.SkillLevelEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "anonymous_player_reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"lobby_id", "reviewer_hash", "target_user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnonymousPlayerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lobby_id", nullable = false)
    private TeamLobby lobby;

    // Lưu mã băm SHA-256(lobbyId + reviewerId) để ẩn danh người đánh giá nhưng chống đánh giá trùng
    @Column(name = "reviewer_hash", length = 64, nullable = false)
    private String reviewerHash;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "voted_skill_level", length = 20, nullable = false)
    private SkillLevelEnum votedSkillLevel;

    @Builder.Default
    @Column(name = "is_no_show", nullable = false)
    private Boolean isNoShow = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}