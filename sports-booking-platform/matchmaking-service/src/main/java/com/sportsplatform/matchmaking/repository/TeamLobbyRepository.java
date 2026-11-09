package com.sportsplatform.matchmaking.repository;

import com.sportsplatform.common.enums.SportTypeEnum;
import com.sportsplatform.matchmaking.entity.TeamLobby;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamLobbyRepository extends JpaRepository<TeamLobby, UUID> {
    Optional<TeamLobby> findByBookingId(UUID bookingId);
    List<TeamLobby> findByStatusOrderByPlayDateAscStartTimeAsc(String status);
    List<TeamLobby> findByStatusAndSportTypeOrderByPlayDateAscStartTimeAsc(String status, SportTypeEnum sportType);
}