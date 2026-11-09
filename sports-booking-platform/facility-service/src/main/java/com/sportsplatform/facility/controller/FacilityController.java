package com.sportsplatform.facility.controller;

import com.sportsplatform.common.dto.ApiResponse;
import com.sportsplatform.common.dto.SlotLockResponse;
import com.sportsplatform.facility.dto.*;
import com.sportsplatform.facility.entity.Court;
import com.sportsplatform.facility.entity.Facility;
import com.sportsplatform.facility.service.FacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    // 1. Chủ sân gửi hồ sơ đăng ký đối tác sân
    @PostMapping("/facilities/register")
    public ResponseEntity<ApiResponse<Facility>> registerFacility(
            @RequestHeader("X-User-Id") UUID managerId,
            @Valid @RequestBody RegisterFacilityRequest request) {
        Facility saved = facilityService.registerFacility(managerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã gửi hồ sơ đăng ký cụm sân, chờ Admin duyệt", saved));
    }

    // 2. Admin phê duyệt hoặc từ chối hồ sơ cụm sân
    @PutMapping("/admin/facilities/{id}/approval")
    public ResponseEntity<ApiResponse<Facility>> approveFacility(
            @PathVariable UUID id,
            @RequestParam String status) {
        Facility updated = facilityService.approveOrRejectFacility(id, status);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái cụm sân: " + status, updated));
    }

    // 3. Tìm cụm sân xung quanh theo GPS (Public cho cả Guest và Player)
    @GetMapping("/facilities/nearby")
    public ResponseEntity<ApiResponse<List<Facility>>> getNearbyFacilities(
            @RequestParam(defaultValue = "10.8018") double lat,
            @RequestParam(defaultValue = "106.7144") double lng,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        return ResponseEntity.ok(ApiResponse.success(
                "Danh sách cụm sân trong bán kính " + radiusKm + " km",
                facilityService.findNearbyFacilities(lat, lng, radiusKm)));
    }

    // 4. Chủ sân thêm sân con và cấu hình khung giờ + giá tiền
    @PostMapping("/facilities/{id}/courts")
    public ResponseEntity<ApiResponse<Court>> createCourtWithSlots(
            @RequestHeader("X-User-Id") UUID managerId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCourtWithSlotsRequest request) {
        Court court = facilityService.createCourtAndSlots(managerId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sân con và cấu hình khung giờ thành công", court));
    }

    // 5. Xem trạng thái khung giờ trống/kín của cụm sân theo ngày
    @GetMapping("/facilities/{id}/slots")
    public ResponseEntity<ApiResponse<List<SlotViewResponse>>> getSlotsByDate(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate playDate) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lịch sân ngày " + playDate,
                facilityService.getFacilitySlotsByDate(id, playDate)));
    }

    // 6. [INTERNAL API] Booking Service gọi sang để khóa tạm Slot trên Redis (TTL 5 phút)
    @PostMapping("/internal/slots/{slotId}/lock")
    public ResponseEntity<SlotLockResponse> lockSlot(
            @PathVariable UUID slotId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(facilityService.lockSlotForBooking(slotId, userId));
    }

    // 7. [INTERNAL API] Booking Service gọi sang để cập nhật trạng thái BOOKED / AVAILABLE
    @PutMapping("/internal/slots/{slotId}/status")
    public ResponseEntity<Void> updateSlotStatus(
            @PathVariable UUID slotId,
            @RequestParam String status,
            @RequestParam(required = false) UUID bookingId) {
        facilityService.updateSlotStatus(slotId, status, bookingId);
        return ResponseEntity.ok().build();
    }

    // 8. [INTERNAL API] Lưu đánh giá 1-5 sao cho cụm sân sau trận đấu
    @PostMapping("/internal/facilities/{id}/reviews")
    public ResponseEntity<Void> addReview(
            @PathVariable UUID id,
            @Valid @RequestBody CreateFacilityReviewRequest request) {
        facilityService.addFacilityReview(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}