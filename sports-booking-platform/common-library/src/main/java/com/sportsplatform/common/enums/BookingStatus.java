package com.sportsplatform.common.enums;

public enum BookingStatus {
    PENDING,      // Đang khóa Redis 5 phút chờ thanh toán cọc 50%
    BOOKED,       // Đã thanh toán cọc thành công (Duyệt tự động)
    CHECKED_IN,   // Chủ sân đã quét mã QR tại sân
    COMPLETED,    // Chủ sân đã bấm "Đã thu đủ 50% còn lại"
    CANCELLED,    // Đơn đã hủy (Người chơi hủy hoặc Chủ sân hủy do thời tiết)
    NO_SHOW       // Khách vắng mặt không đến nhận sân
}

