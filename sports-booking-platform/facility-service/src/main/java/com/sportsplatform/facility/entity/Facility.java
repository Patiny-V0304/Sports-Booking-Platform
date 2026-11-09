package com.sportsplatform.facility.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "facilities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "manager_id", nullable = false)
    private UUID managerId; // Logical FK trỏ sang users.id

    @Column(length = 150, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String address;

    @Column(precision = 9, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6, nullable = false)
    private BigDecimal longitude;

    @Column(name = "business_license_url", columnDefinition = "TEXT", nullable = false)
    private String businessLicenseUrl;

    @Builder.Default
    @Column(name = "is_outdoor", nullable = false)
    private Boolean isOutdoor = false;

    @Builder.Default
    @Column(name = "rating_avg", precision = 3, scale = 2, nullable = false)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Builder.Default
    @Column(length = 25, nullable = false)
    private String status = "PENDING_APPROVAL"; // PENDING_APPROVAL | ACTIVE | REJECTED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}