package com.sportsplatform.identity.controller;

import com.sportsplatform.common.dto.ApiResponse;
import com.sportsplatform.identity.dto.*;
import com.sportsplatform.identity.entity.SportProfile;
import com.sportsplatform.identity.service.IdentityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse res = identityService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", res));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse res = identityService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", res));
    }

    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ thành công", identityService.getMyProfile(userId)));
    }

    @PutMapping("/users/me/vietqr")
    public ResponseEntity<ApiResponse<String>> updateVietQr(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateVietQrRequest request) {
        identityService.updateVietQr(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mã VietQR nhận tiền thành công", null));
    }

    @PutMapping("/profiles/sports")
    public ResponseEntity<ApiResponse<SportProfile>> updateSportProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateSportProfileRequest request) {
        SportProfile updated = identityService.updateSportProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thể thao thành công", updated));
    }

    // Endpoint nội bộ để các Microservice khác gọi sang cộng/trừ điểm uy tín
    @PostMapping("/internal/users/{userId}/reliability")
    public ResponseEntity<ApiResponse<Void>> adjustReliability(
            @PathVariable UUID userId,
            @RequestParam int delta,
            @RequestParam String reason,
            @RequestParam(required = false) UUID bookingId) {
        identityService.adjustReliabilityScore(userId, delta, reason, bookingId);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật điểm uy tín", null));
    }
}