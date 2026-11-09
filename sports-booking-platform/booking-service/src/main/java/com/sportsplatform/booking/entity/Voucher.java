package com.sportsplatform.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 30, unique = true, nullable = false)
    private String code;

    @Column(name = "issuer_type", length = 20, nullable = false)
    private String issuerType; // FACILITY (Trừ doanh thu sân) | PLATFORM (Trừ phí sàn)

    @Column(name = "facility_id")
    private UUID facilityId; // Null nếu là mã toàn sàn của Admin

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @Column(name = "max_discount_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal maxDiscountAmount;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "usage_limit", nullable = false)
    private Integer usageLimit;

    @Builder.Default
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;
}