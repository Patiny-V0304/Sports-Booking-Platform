package com.sportsplatform.matchmaking.repository;

import com.sportsplatform.matchmaking.entity.LobbyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LobbyMemberRepository extends JpaRepository<LobbyMember, UUID> {
    List<LobbyMember> findByLobbyId(UUID lobbyId);
    Optional<LobbyMember> findByLobbyIdAndUserId(UUID lobbyId, UUID userId);
    boolean existsByLobbyIdAndUserId(UUID lobbyId, UUID userId);
}