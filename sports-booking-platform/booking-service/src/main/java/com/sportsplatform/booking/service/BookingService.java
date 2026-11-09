package com.sportsplatform.booking.service;

import com.sportsplatform.booking.client.FacilityClient;
import com.sportsplatform.booking.client.IdentityClient;
import com.sportsplatform.booking.dto.*;
import com.sportsplatform.booking.entity.Booking;
import com.sportsplatform.booking.entity.PaymentTransaction;
import com.sportsplatform.booking.entity.Voucher;
import com.sportsplatform.booking.repository.BookingRepository;
import com.sportsplatform.booking.repository.PaymentTransactionRepository;
import com.sportsplatform.booking.repository.VoucherRepository;
import com.sportsplatform.common.dto.SlotLockResponse;
import com.sportsplatform.common.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VoucherRepository voucherRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final FacilityClient facilityClient;
    private final IdentityClient identityClient;
    private final com.sportsplatform.booking.messaging.BookingEventPublisher eventPublisher;

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.10"); // Phí sàn mặc định 10%

    /**
     * 1. Tạo đơn đặt sân, khóa Redis 5 phút và tính tiền cọc 50%
     */
    @Transactional
    public Map<String, Object> createBooking(UUID bookerId, CreateBookingRequest req) {
        // Gọi sang Facility Service khóa tạm Slot trên Redis (5 phút)
        SlotLockResponse slotInfo = facilityClient.lockSlot(req.slotId(), bookerId);

        BigDecimal courtPrice = slotInfo.price();
        BigDecimal facilityDiscount = BigDecimal.ZERO;
        BigDecimal platformDiscount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;

        // Xử lý bóc tách mã giảm giá của Chủ sân (FACILITY) và Khuyến mãi của Admin (PLATFORM)
        if (req.voucherCode() != null && !req.voucherCode().isBlank()) {
            Voucher voucher = voucherRepository.findByCodeIgnoreCase(req.voucherCode())
                    .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại"));

            if (voucher.getValidTo().isBefore(LocalDateTime.now()) || voucher.getUsedCount() >= voucher.getUsageLimit()) {
                throw new IllegalArgumentException("Mã giảm giá đã hết hạn hoặc hết lượt sử dụng");
            }
            if ("FACILITY".equals(voucher.getIssuerType()) && !slotInfo.facilityId().equals(voucher.getFacilityId())) {
                throw new IllegalArgumentException("Mã giảm giá này không áp dụng cho cụm sân hiện tại");
            }

            BigDecimal rawDiscount = courtPrice.multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal discount = rawDiscount.min(voucher.getMaxDiscountAmount());

            if ("FACILITY".equals(voucher.getIssuerType())) {
                facilityDiscount = discount;
            } else {
                platformDiscount = discount;
            }
            voucher.setUsedCount(voucher.getUsedCount() + 1);
            voucherRepository.save(voucher);
            appliedVoucher = voucher;
        }

        // Tính tiền sau giảm giá: 50% cọc qua hệ thống - 50% trả tại sân
        BigDecimal finalPrice = courtPrice.subtract(facilityDiscount).subtract(platformDiscount).max(BigDecimal.ZERO);
        BigDecimal depositAmount = finalPrice.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal remainingAmount = finalPrice.subtract(depositAmount);

        // Phí sàn thu của Chủ sân (Khuyến mãi của Admin được trừ vào phí sàn này)
        BigDecimal basePlatformFee = courtPrice.multiply(PLATFORM_FEE_RATE);
        BigDecimal actualPlatformFee = basePlatformFee.subtract(platformDiscount).max(BigDecimal.ZERO);
        BigDecimal netOwnerDepositRev = depositAmount.subtract(actualPlatformFee);

        String bookingCode = "BK-" + System.currentTimeMillis();
        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .bookerId(bookerId)
                .facilityId(slotInfo.facilityId())
                .courtId(slotInfo.courtId())
                .slotId(req.slotId())
                .voucher(appliedVoucher)
                .playDate(slotInfo.playDate())
                .startTime(slotInfo.startTime())
                .endTime(slotInfo.endTime())
                .hasMatchmaking(req.hasMatchmaking())
                .courtPrice(courtPrice)
                .facilityDiscount(facilityDiscount)
                .platformDiscount(platformDiscount)
                .depositAmount(depositAmount)
                .remainingAmount(remainingAmount)
                .platformFee(actualPlatformFee)
                .netOwnerDepositRev(netOwnerDepositRev)
                .qrCheckinToken("QR-" + UUID.randomUUID())
                .rescheduleCount(0)
                .status(BookingStatus.PENDING)
                .build();

        bookingRepository.save(booking);

        String gateway = req.paymentGateway() != null ? req.paymentGateway() : "VNPAY";
        String mockPaymentUrl = "https://sandbox.vnpayment.vn/pay?bookingCode=" + bookingCode + "&amount=" + depositAmount;

        Map<String, Object> result = new HashMap<>();
        result.put("booking", booking);
        result.put("lockExpiresInSeconds", 300);
        result.put("paymentGateway", gateway);
        result.put("paymentUrl", mockPaymentUrl);
        return result;
    }

    /**
     * 2. Xử lý Webhook từ Cổng thanh toán -> Duyệt tự động sang BOOKED
     */
    @Transactional
    public Booking handlePaymentWebhook(PaymentWebhookRequest req) {
        Booking booking = bookingRepository.findByBookingCode(req.bookingCode())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân: " + req.bookingCode()));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Đơn đặt sân đã được xử lý trước đó");
        }

        boolean isSuccess = "00".equals(req.responseCode());

        PaymentTransaction txn = PaymentTransaction.builder()
                .booking(booking)
                .transactionType("DEPOSIT_PAYMENT")
                .gatewayName("VNPAY")
                .gatewayTxnRef(req.gatewayTxnRef())
                .amount(booking.getDepositAmount())
                .status(isSuccess ? "SUCCESS" : "FAILED")
                .build();
        paymentTransactionRepository.save(txn);

        if (isSuccess) {
            booking.setStatus(BookingStatus.BOOKED);
            facilityClient.updateSlotStatus(booking.getSlotId(), "BOOKED", booking.getId());
            eventPublisher.publishBookingEvent("confirmed", new com.sportsplatform.common.dto.BookingEventPayload(
                    "BOOKING_CONFIRMED", booking.getId(), booking.getBookingCode(),
                    booking.getBookerId(), java.math.BigDecimal.ZERO,
                    "Đặt sân thành công! Mã QR Check-in đã sẵn sàng."
            ));
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelReason("PAYMENT_FAILED");
            facilityClient.updateSlotStatus(booking.getSlotId(), "AVAILABLE", null);
        }
        return bookingRepository.save(booking);
    }

    /**
     * 3. Xem trước mức hoàn tiền cọc theo mốc giờ quy định (Cancellation Policy Engine)
     */
    public CancelPreviewResponse previewCancellation(UUID bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime playDateTime = LocalDateTime.of(booking.getPlayDate(), booking.getStartTime());
        double hoursUntilPlay = Duration.between(now, playDateTime).toMinutes() / 60.0;
        long minutesSinceCreated = Duration.between(booking.getCreatedAt(), now).toMinutes();

        int refundPercent;
        String policyRule;

        if ("WEATHER_CANCEL".equalsIgnoreCase(reason)) {
            refundPercent = 100;
            policyRule = "WEATHER_FORCE_MAJEURE_100_PERCENT";
        } else if ("INSUFFICIENT_PLAYERS_4H".equalsIgnoreCase(reason) && hoursUntilPlay >= 4.0) {
            refundPercent = 100;
            policyRule = "INSUFFICIENT_TEAM_PLAYERS_AT_4H_MARK";
        } else if (minutesSinceCreated <= 30 && hoursUntilPlay >= 2.0) {
            refundPercent = 100;
            policyRule = "GRACE_PERIOD_30_MINS_100_PERCENT";
        } else if (hoursUntilPlay >= 6.0) {
            refundPercent = 100;
            policyRule = "CANCEL_BEFORE_6_HOURS_100_PERCENT";
        } else if (hoursUntilPlay >= 2.0) {
            refundPercent = 50;
            policyRule = "CANCEL_BETWEEN_2_AND_6_HOURS_50_PERCENT";
        } else {
            refundPercent = 0;
            policyRule = "LATE_CANCEL_UNDER_2_HOURS_NO_REFUND";
        }

        BigDecimal refundAmount = booking.getDepositAmount()
                .multiply(BigDecimal.valueOf(refundPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        boolean canReschedule = booking.getRescheduleCount() == 0 && hoursUntilPlay >= 1.0;

        return new CancelPreviewResponse(
                booking.getId(),
                booking.getBookingCode(),
                Math.round(hoursUntilPlay * 10.0) / 10.0,
                policyRule,
                refundPercent,
                booking.getDepositAmount(),
                refundAmount,
                canReschedule
        );
    }

    /**
     * 4. Xác nhận Hủy đơn đặt sân và hoàn tiền cọc
     */
    @Transactional
    public Booking cancelBooking(UUID bookingId, CancelBookingRequest req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân"));

        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new IllegalStateException("Chỉ có thể hủy đơn đang ở trạng thái BOOKED");
        }

        CancelPreviewResponse preview = previewCancellation(bookingId, req.cancelReason());

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelReason(req.cancelReason());
        booking.setRefundAmount(preview.estimatedRefundAmount());
        bookingRepository.save(booking);

        // Nếu có tiền hoàn cọc -> Ghi nhận giao dịch REFUND
        if (preview.estimatedRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
            PaymentTransaction refundTxn = PaymentTransaction.builder()
                    .booking(booking)
                    .transactionType("REFUND")
                    .gatewayName("VNPAY")
                    .gatewayTxnRef("REFUND-" + System.currentTimeMillis())
                    .amount(preview.estimatedRefundAmount())
                    .status("SUCCESS")
                    .build();
            paymentTransactionRepository.save(refundTxn);
        }

        // Giải phóng Slot sân về lại AVAILABLE trên Facility Service
        facilityClient.updateSlotStatus(booking.getSlotId(), "AVAILABLE", null);

        eventPublisher.publishBookingEvent("cancelled", new com.sportsplatform.common.dto.BookingEventPayload(
                "BOOKING_CANCELLED", booking.getId(), booking.getBookingCode(),
                booking.getBookerId(), preview.estimatedRefundAmount(),
                "Đơn đặt sân đã hủy. Tự động đóng phòng ghép kèo liên kết."
        ));
        return booking;
    }

    /**
     * 5. Đổi lịch sân miễn phí tối đa 1 lần (Trước giờ chơi >= 1 tiếng)
     */
    @Transactional
    public Booking rescheduleBooking(UUID bookerId, UUID bookingId, UUID newSlotId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân"));

        if (!booking.getBookerId().equals(bookerId)) {
            throw new IllegalStateException("Bạn không phải người tạo đơn đặt sân này");
        }
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new IllegalStateException("Chỉ đơn BOOKED mới được phép đổi lịch");
        }
        if (booking.getRescheduleCount() >= 1) {
            throw new IllegalStateException("Đơn đặt sân này đã sử dụng hết 1 lần đổi lịch miễn phí");
        }

        LocalDateTime playDateTime = LocalDateTime.of(booking.getPlayDate(), booking.getStartTime());
        if (Duration.between(LocalDateTime.now(), playDateTime).toMinutes() < 60) {
            throw new IllegalStateException("Chỉ được phép đổi lịch trước giờ thi đấu tối thiểu 1 tiếng");
        }

        // Khóa và lấy thông tin Slot mới
        SlotLockResponse newSlot = facilityClient.lockSlot(newSlotId, bookerId);
        if (newSlot.price().compareTo(booking.getCourtPrice()) != 0) {
            facilityClient.updateSlotStatus(newSlotId, "AVAILABLE", null);
            throw new IllegalArgumentException("Khung giờ mới phải có cùng mệnh giá với khung giờ cũ");
        }

        // Giải phóng Slot cũ và chốt Slot mới
        facilityClient.updateSlotStatus(booking.getSlotId(), "AVAILABLE", null);
        facilityClient.updateSlotStatus(newSlotId, "BOOKED", booking.getId());

        booking.setSlotId(newSlotId);
        booking.setCourtId(newSlot.courtId());
        booking.setPlayDate(newSlot.playDate());
        booking.setStartTime(newSlot.startTime());
        booking.setEndTime(newSlot.endTime());
        booking.setRescheduleCount(booking.getRescheduleCount() + 1);

        return bookingRepository.save(booking);
    }

    /**
     * 6. Chủ sân quét mã QR Check-in của khách tại sân
     */
    @Transactional
    public Booking verifyQrCheckIn(String qrToken) {
        Booking booking = bookingRepository.findByQrCheckinToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Mã QR Check-in không hợp lệ"));

        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new IllegalStateException("Đơn đặt sân không ở trạng thái hợp lệ để Check-in: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    /**
     * 7. Chủ sân bấm "Đã thu đủ 50% còn lại" -> Hoàn tất đơn & Chốt doanh thu
     */
    @Transactional
    public Booking completeBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân"));

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Khách phải được quét QR Check-in trước khi bấm Đã thu đủ");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        // Cộng +2 điểm uy tín cho người đặt khi hoàn thành trận đấu đúng hẹn
        identityClient.adjustReliability(booking.getBookerId(), 2, "COMPLETED", booking.getId());
        return bookingRepository.save(booking);
    }

    /**
     * 8. Chủ sân đánh dấu khách vắng mặt (No-show) -> Trừ 20 điểm uy tín của khách
     */
    @Transactional
    public Booking markNoShow(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân"));

        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new IllegalStateException("Chỉ đánh dấu No-show cho đơn chưa Check-in");
        }

        booking.setStatus(BookingStatus.NO_SHOW);
        bookingRepository.save(booking);

        // Trừ 20 điểm uy tín bên Identity Service
        identityClient.adjustReliability(booking.getBookerId(), -20, "NO_SHOW", booking.getId());
        return booking;
    }

    @Transactional
    public Voucher createVoucher(CreateVoucherRequest req) {
        Voucher voucher = Voucher.builder()
                .code(req.code().toUpperCase())
                .issuerType(req.issuerType())
                .facilityId(req.facilityId())
                .discountPercent(req.discountPercent())
                .maxDiscountAmount(req.maxDiscountAmount())
                .validFrom(LocalDateTime.now())
                .validTo(req.validTo())
                .usageLimit(req.usageLimit())
                .usedCount(0)
                .build();
        return voucherRepository.save(voucher);
    }

    public Booking getBookingById(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân"));
    }

    public Map<String, Object> getFacilityRevenueReport(UUID facilityId) {
        List<Booking> completed = bookingRepository.findByFacilityIdAndStatus(facilityId, BookingStatus.COMPLETED);
        BigDecimal totalGrossRevenue = completed.stream()
                .map(Booking::getCourtPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNetDepositFromSystem = completed.stream()
                .map(Booking::getNetOwnerDepositRev)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollectedAtCourt = completed.stream()
                .map(Booking::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new HashMap<>();
        report.put("facilityId", facilityId);
        report.put("completedBookingsCount", completed.size());
        report.put("totalGrossRevenue", totalGrossRevenue);
        report.put("totalNetDepositFromSystem", totalNetDepositFromSystem);
        report.put("totalCollectedAtCourt", totalCollectedAtCourt);
        return report;
    }
}