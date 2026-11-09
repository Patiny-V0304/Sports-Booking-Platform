package com.sportsplatform.matchmaking.controller;

import com.sportsplatform.common.dto.ApiResponse;
import com.sportsplatform.common.enums.SportTypeEnum;
import com.sportsplatform.matchmaking.dto.CreateLobbyRequest;
import com.sportsplatform.matchmaking.dto.PostMatchReviewRequest;
import com.sportsplatform.matchmaking.entity.LobbyMember;
import com.sportsplatform.matchmaking.entity.TeamLobby;
import com.sportsplatform.matchmaking.service.MatchmakingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    // 1. Xem danh sách phòng ghép kèo đang mở (Public cho cả Guest và Player)
    @GetMapping("/lobbies")
    public ResponseEntity<ApiResponse<List<TeamLobby>>> getOpenLobbies(
            @RequestParam(required = false) SportTypeEnum sportType) {
        return ResponseEntity.ok(ApiResponse.success("Danh sách phòng ghép kèo đang mở",
                matchmakingService.getOpenLobbies(sportType)));
    }

    // 2. Chủ phòng (Host) tạo phòng ghép kèo từ đơn đặt sân
    @PostMapping("/lobbies")
    public ResponseEntity<ApiResponse<TeamLobby>> createLobby(
            @RequestHeader("X-User-Id") UUID hostId,
            @Valid @RequestBody CreateLobbyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mở phòng ghép kèo thành công",
                        matchmakingService.createLobby(hostId, request)));
    }

    // 3. Người chơi lẻ (Member) xin tham gia phòng
    @PostMapping("/lobbies/{id}/join")
    public ResponseEntity<ApiResponse<LobbyMember>> requestJoin(
            @RequestHeader("X-User-Id") UUID memberId,
            @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã gửi yêu cầu tham gia phòng, chờ Chủ phòng duyệt",
                        matchmakingService.requestJoinLobby(memberId, id)));
    }

    // 4. Chủ phòng Duyệt hoặc Từ chối thành viên
    @PutMapping("/lobbies/{id}/members/{memberUserId}/approval")
    public ResponseEntity<ApiResponse<LobbyMember>> approveOrReject(
            @RequestHeader("X-User-Id") UUID hostId,
            @PathVariable UUID id,
            @PathVariable UUID memberUserId,
            @RequestParam String decision) {
        return ResponseEntity.ok(ApiResponse.success("Đã xử lý yêu cầu tham gia: " + decision,
                matchmakingService.approveOrRejectMember(hostId, id, memberUserId, decision)));
    }

    // 5. Thành viên bấm "Báo đã chuyển khoản" tiền chia sẻ cho Host
    @PostMapping("/lobbies/{id}/report-paid")
    public ResponseEntity<ApiResponse<LobbyMember>> reportPaid(
            @RequestHeader("X-User-Id") UUID memberId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Đã thông báo chuyển khoản cho Chủ phòng",
                matchmakingService.reportTransferredToHost(memberId, id)));
    }

    // 6. Chủ phòng bấm "Đã nhận tiền" từ thành viên
    @PutMapping("/lobbies/{id}/members/{memberUserId}/confirm-paid")
    public ResponseEntity<ApiResponse<LobbyMember>> confirmPaid(
            @RequestHeader("X-User-Id") UUID hostId,
            @PathVariable UUID id,
            @PathVariable UUID memberUserId) {
        return ResponseEntity.ok(ApiResponse.success("Chủ phòng đã xác nhận nhận đủ tiền chia sẻ",
                matchmakingService.confirmPaymentReceived(hostId, id, memberUserId)));
    }

    // 7. Gửi Đánh giá sân (1-5 sao) & Đánh giá ẩn danh thành viên sau trận đấu
    @PostMapping("/reviews/post-match")
    public ResponseEntity<ApiResponse<Void>> submitPostMatchReview(
            @RequestHeader("X-User-Id") UUID reviewerId,
            @Valid @RequestBody PostMatchReviewRequest request) {
        matchmakingService.submitPostMatchReviews(reviewerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cảm ơn bạn đã gửi đánh giá trận đấu!", null));
    }
}