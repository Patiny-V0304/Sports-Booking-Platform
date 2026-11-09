package com.sportsplatform.matchmaking.service;

import com.sportsplatform.common.enums.BookingStatus;
import com.sportsplatform.common.enums.SportTypeEnum;
import com.sportsplatform.matchmaking.client.BookingClient;
import com.sportsplatform.matchmaking.client.FacilityClient;
import com.sportsplatform.matchmaking.client.IdentityClient;
import com.sportsplatform.matchmaking.dto.*;
import com.sportsplatform.matchmaking.entity.AnonymousPlayerReview;
import com.sportsplatform.matchmaking.entity.LobbyMember;
import com.sportsplatform.matchmaking.entity.TeamLobby;
import com.sportsplatform.matchmaking.repository.AnonymousPlayerReviewRepository;
import com.sportsplatform.matchmaking.repository.LobbyMemberRepository;
import com.sportsplatform.matchmaking.repository.TeamLobbyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final TeamLobbyRepository teamLobbyRepository;
    private final LobbyMemberRepository lobbyMemberRepository;
    private final AnonymousPlayerReviewRepository reviewRepository;
    private final BookingClient bookingClient;
    private final FacilityClient facilityClient;
    private final IdentityClient identityClient;

    /**
     * 1. Chủ phòng (Host) mở phòng ghép kèo từ đơn đặt sân đã BOOKED
     */
    @Transactional
    public TeamLobby createLobby(UUID hostId, CreateLobbyRequest req) {
        if (teamLobbyRepository.findByBookingId(req.bookingId()).isPresent()) {
            throw new IllegalStateException("Đơn đặt sân này đã được mở phòng ghép kèo trước đó");
        }

        // Gọi sang Booking Service kiểm tra đơn đặt sân
        BookingInfoDto booking = bookingClient.getBookingById(req.bookingId());
        if (!booking.bookerId().equals(hostId)) {
            throw new IllegalStateException("Bạn không phải người đứng tên đơn đặt sân này");
        }
        if (booking.status() != BookingStatus.BOOKED) {
            throw new IllegalStateException("Chỉ đơn đặt sân đã thanh toán cọc (BOOKED) mới được mở phòng");
        }

        TeamLobby lobby = TeamLobby.builder()
                .bookingId(booking.id())
                .hostId(hostId)
                .facilityId(booking.facilityId())
                .sportType(req.sportType())
                .playDate(booking.playDate())
                .startTime(booking.startTime())
                .endTime(booking.endTime())
                .maxRecruits(req.maxRecruits())
                .currentRecruits(0)
                .requiredSkillLevel(req.requiredSkillLevel())
                .shareAmountPerPerson(req.shareAmountPerPerson())
                .hostVietqrUrl(req.hostVietqrUrl())
                .status("OPEN")
                .build();
        teamLobbyRepository.save(lobby);

        // Ghi nhận Host là thành viên gốc của phòng
        LobbyMember hostMember = LobbyMember.builder()
                .lobby(lobby)
                .userId(hostId)
                .roleInLobby("HOST")
                .joinStatus("APPROVED")
                .paymentStatus("PAID")
                .paidConfirmedAt(LocalDateTime.now())
                .build();
        lobbyMemberRepository.save(hostMember);

        return lobby;
    }

    /**
     * 2. Tìm kiếm danh sách phòng ghép kèo đang mở (OPEN)
     */
    public List<TeamLobby> getOpenLobbies(SportTypeEnum sportType) {
        if (sportType != null) {
            return teamLobbyRepository.findByStatusAndSportTypeOrderByPlayDateAscStartTimeAsc("OPEN", sportType);
        }
        return teamLobbyRepository.findByStatusOrderByPlayDateAscStartTimeAsc("OPEN");
    }

    /**
     * 3. Người chơi lẻ (Member) gửi yêu cầu xin vào phòng
     */
    @Transactional
    public LobbyMember requestJoinLobby(UUID memberUserId, UUID lobbyId) {
        TeamLobby lobby = teamLobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ghép kèo"));

        if (!"OPEN".equals(lobby.getStatus()) || lobby.getCurrentRecruits() >= lobby.getMaxRecruits()) {
            throw new IllegalStateException("Phòng ghép kèo đã đủ số lượng thành viên cố định (FULL)");
        }
        if (lobbyMemberRepository.existsByLobbyIdAndUserId(lobbyId, memberUserId)) {
            throw new IllegalStateException("Bạn đã gửi yêu cầu hoặc đã ở trong phòng này");
        }

        LobbyMember member = LobbyMember.builder()
                .lobby(lobby)
                .userId(memberUserId)
                .roleInLobby("MEMBER")
                .joinStatus("PENDING_APPROVAL")
                .paymentStatus("UNPAID")
                .build();
        return lobbyMemberRepository.save(member);
    }

    /**
     * 4. Chủ phòng (Host) Duyệt hoặc Từ chối thành viên
     */
    @Transactional
    public LobbyMember approveOrRejectMember(UUID hostId, UUID lobbyId, UUID memberUserId, String decision) {
        TeamLobby lobby = teamLobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ghép kèo"));

        if (!lobby.getHostId().equals(hostId)) {
            throw new IllegalStateException("Chỉ Chủ phòng (Host) mới có quyền duyệt thành viên");
        }

        LobbyMember member = lobbyMemberRepository.findByLobbyIdAndUserId(lobbyId, memberUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu tham gia của người chơi này"));

        if (!"PENDING_APPROVAL".equals(member.getJoinStatus())) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý trước đó");
        }

        if ("APPROVED".equalsIgnoreCase(decision)) {
            if (lobby.getCurrentRecruits() >= lobby.getMaxRecruits()) {
                throw new IllegalStateException("Phòng đã đủ số lượng người tuyển");
            }
            member.setJoinStatus("APPROVED");
            lobby.setCurrentRecruits(lobby.getCurrentRecruits() + 1);
            if (lobby.getCurrentRecruits() >= lobby.getMaxRecruits()) {
                lobby.setStatus("FULL");
            }
            teamLobbyRepository.save(lobby);
        } else if ("REJECTED".equalsIgnoreCase(decision)) {
            member.setJoinStatus("REJECTED");
        } else {
            throw new IllegalArgumentException("Quyết định chỉ nhận APPROVED hoặc REJECTED");
        }

        return lobbyMemberRepository.save(member);
    }

    /**
     * 5. Thành viên bấm "Báo đã chuyển khoản" sau khi quét mã VietQR của Host
     */
    @Transactional
    public LobbyMember reportTransferredToHost(UUID memberUserId, UUID lobbyId) {
        LobbyMember member = lobbyMemberRepository.findByLobbyIdAndUserId(lobbyId, memberUserId)
                .orElseThrow(() -> new IllegalArgumentException("Bạn không phải thành viên của phòng này"));

        if (!"APPROVED".equals(member.getJoinStatus())) {
            throw new IllegalStateException("Bạn phải được Chủ phòng duyệt vào đội trước khi báo chuyển khoản");
        }

        member.setPaymentStatus("REPORTED_PAID");
        return lobbyMemberRepository.save(member);
    }

    /**
     * 6. Chủ phòng bấm "Đã nhận tiền" từ thành viên -> Chuyển sang PAID
     */
    @Transactional
    public LobbyMember confirmPaymentReceived(UUID hostId, UUID lobbyId, UUID memberUserId) {
        TeamLobby lobby = teamLobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ghép kèo"));

        if (!lobby.getHostId().equals(hostId)) {
            throw new IllegalStateException("Chỉ Chủ phòng mới có quyền xác nhận đã nhận tiền");
        }

        LobbyMember member = lobbyMemberRepository.findByLobbyIdAndUserId(lobbyId, memberUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thành viên trong phòng"));

        member.setPaymentStatus("PAID");
        member.setPaidConfirmedAt(LocalDateTime.now());
        return lobbyMemberRepository.save(member);
    }

    /**
     * 7. Đánh giá sân (1-5 sao) & Đánh giá ẩn danh thành viên sau trận đấu (SHA-256)
     */
    @Transactional
    public void submitPostMatchReviews(UUID reviewerId, PostMatchReviewRequest req) {
        BookingInfoDto booking = bookingClient.getBookingById(req.bookingId());
        if (booking.status() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Chỉ được phép gửi đánh giá sau khi trận đấu đã hoàn tất (COMPLETED)");
        }

        // 7a. Nếu có đánh giá chất lượng cụm sân (1-5 sao) -> Gọi sang Facility Service
        if (req.facilityReview() != null) {
            facilityClient.submitFacilityReview(booking.facilityId(), new FacilityReviewInternalRequest(
                    booking.id(),
                    reviewerId,
                    req.facilityReview().starRating(),
                    req.facilityReview().comment()
            ));
        }

        // 7b. Nếu có đánh giá ẩn danh thành viên cùng đội
        if (req.lobbyId() != null && req.anonymousPlayerReviews() != null && !req.anonymousPlayerReviews().isEmpty()) {
            TeamLobby lobby = teamLobbyRepository.findById(req.lobbyId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ghép kèo"));

            String reviewerHash = sha256Hash(lobby.getId() + ":" + reviewerId);

            for (PostMatchReviewRequest.PlayerReviewItem item : req.anonymousPlayerReviews()) {
                if (item.targetUserId().equals(reviewerId)) {
                    throw new IllegalArgumentException("Bạn không thể tự đánh giá chính mình");
                }
                if (reviewRepository.existsByLobbyIdAndReviewerHashAndTargetUserId(
                        lobby.getId(), reviewerHash, item.targetUserId())) {
                    throw new IllegalStateException("Bạn đã đánh giá thành viên này trong trận đấu rồi");
                }

                AnonymousPlayerReview review = AnonymousPlayerReview.builder()
                        .lobby(lobby)
                        .reviewerHash(reviewerHash)
                        .targetUserId(item.targetUserId())
                        .votedSkillLevel(item.votedSkillLevel())
                        .isNoShow(item.isNoShow())
                        .build();
                reviewRepository.save(review);

                // Nếu bị đồng đội gắn cờ Vắng mặt (No-show) -> Trừ 15 điểm uy tín bên Identity Service
                if (item.isNoShow()) {
                    identityClient.adjustReliability(item.targetUserId(), -15, "TEAM_NO_SHOW", booking.id());
                }
            }
        }
    }

    private String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không thể khởi tạo thuật toán SHA-256", e);
        }
    }
}