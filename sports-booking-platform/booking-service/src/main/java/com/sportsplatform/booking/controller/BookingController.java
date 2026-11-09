package com.sportsplatform.booking.controller;

import com.sportsplatform.booking.dto.*;
import com.sportsplatform.booking.entity.Booking;
import com.sportsplatform.booking.entity.Voucher;
import com.sportsplatform.booking.service.BookingService;
import com.sportsplatform.common.dto.ApiResponse;
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
public class BookingController {

    private final BookingService bookingService;

    // 1. Tạo đơn đặt sân & Khóa Redis 5 phút
    @PostMapping("/bookings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBooking(
            @RequestHeader("X-User-Id") UUID bookerId,
            @Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã khóa chỗ 5 phút, vui lòng thanh toán cọc 50%",
                        bookingService.createBooking(bookerId, request)));
    }

    // 2. Webhook Cổng thanh toán báo kết quả giao dịch cọc 50%
    @PostMapping("/payments/webhook")
    public ResponseEntity<ApiResponse<Booking>> paymentWebhook(
            @Valid @RequestBody PaymentWebhookRequest request) {
        Booking updated = bookingService.handlePaymentWebhook(request);
        return ResponseEntity.ok(ApiResponse.success("Xử lý Webhook thanh toán thành công", updated));
    }

    // 3. Xem trước mức hoàn tiền cọc khi muốn hủy đơn
    @GetMapping("/bookings/{id}/cancel-preview")
    public ResponseEntity<ApiResponse<CancelPreviewResponse>> previewCancel(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "USER_CANCEL") String reason) {
        return ResponseEntity.ok(ApiResponse.success("Thông tin tính toán hoàn cọc",
                bookingService.previewCancellation(id, reason)));
    }

    // 4. Xác nhận Hủy đơn đặt sân
    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancelBooking(
            @PathVariable UUID id,
            @Valid @RequestBody CancelBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn đặt sân và xử lý hoàn cọc",
                bookingService.cancelBooking(id, request)));
    }

    // 5. Đổi lịch sân miễn phí 1 lần (Trước 1 tiếng)
    @PutMapping("/bookings/{id}/reschedule")
    public ResponseEntity<ApiResponse<Booking>> reschedule(
            @RequestHeader("X-User-Id") UUID bookerId,
            @PathVariable UUID id,
            @RequestParam UUID newSlotId) {
        return ResponseEntity.ok(ApiResponse.success("Đổi lịch sân thành công",
                bookingService.rescheduleBooking(bookerId, id, newSlotId)));
    }

    // 6. Chủ sân quét mã QR Check-in
    @PostMapping("/bookings/verify-qr")
    public ResponseEntity<ApiResponse<Booking>> verifyQr(@RequestParam String qrToken) {
        Booking checkedIn = bookingService.verifyQrCheckIn(qrToken);
        return ResponseEntity.ok(ApiResponse.success(
                "Check-in thành công! Số tiền 50% còn lại cần thu tại sân: " + checkedIn.getRemainingAmount(),
                checkedIn));
    }

    // 7. Chủ sân bấm "Đã thu đủ 50% còn lại" -> Chốt doanh thu
    @PostMapping("/bookings/{id}/complete")
    public ResponseEntity<ApiResponse<Booking>> completeBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận thu đủ tiền và chốt doanh thu đơn",
                bookingService.completeBooking(id)));
    }

    // 8. Chủ sân đánh dấu khách vắng mặt (No-show)
    @PostMapping("/bookings/{id}/no-show")
    public ResponseEntity<ApiResponse<Booking>> markNoShow(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận khách vắng mặt và trừ điểm uy tín",
                bookingService.markNoShow(id)));
    }

    // 9. Tạo mã giảm giá (Chủ sân hoặc Admin)
    @PostMapping("/vouchers")
    public ResponseEntity<ApiResponse<Voucher>> createVoucher(
            @Valid @RequestBody CreateVoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mã giảm giá thành công", bookingService.createVoucher(request)));
    }

    // 10. Xem báo cáo doanh thu của Cụm sân
    @GetMapping("/bookings/owner-revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueReport(
            @RequestParam UUID facilityId) {
        return ResponseEntity.ok(ApiResponse.success("Báo cáo doanh thu cụm sân",
                bookingService.getFacilityRevenueReport(facilityId)));
    }

    // 11. [INTERNAL API] Matchmaking Service gọi sang để kiểm tra đơn đặt sân khi tạo phòng
    @GetMapping("/internal/bookings/{id}")
    public ResponseEntity<Booking> getBookingInternal(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }
}