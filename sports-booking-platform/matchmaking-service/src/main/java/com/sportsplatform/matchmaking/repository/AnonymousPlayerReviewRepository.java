package com.sportsplatform.matchmaking.repository;

import com.sportsplatform.matchmaking.entity.AnonymousPlayerReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnonymousPlayerReviewRepository extends JpaRepository<AnonymousPlayerReview, UUID> {
    boolean existsByLobbyIdAndReviewerHashAndTargetUserId(UUID lobbyId, String reviewerHash, UUID targetUserId);
    List<AnonymousPlayerReview> findByTargetUserId(UUID targetUserId);
}