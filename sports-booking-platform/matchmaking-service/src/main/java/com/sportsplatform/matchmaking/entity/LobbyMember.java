package com.sportsplatform.matchmaking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lobby_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"lobby_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LobbyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lobby_id", nullable = false)
    private TeamLobby lobby;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_in_lobby", length = 15, nullable = false)
    private String roleInLobby; // HOST | MEMBER

    @Column(name = "join_status", length = 20, nullable = false)
    private String joinStatus; // PENDING_APPROVAL | APPROVED | REJECTED | KICKED

    @Builder.Default
    @Column(name = "payment_status", length = 20, nullable = false)
    private String paymentStatus = "UNPAID"; // UNPAID | REPORTED_PAID | PAID

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "paid_confirmed_at")
    private LocalDateTime paidConfirmedAt;
}