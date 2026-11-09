package com.sportsplatform.identity.entity;

import com.sportsplatform.common.enums.RoleEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 15, unique = true, nullable = false)
    private String phone;

    @Column(length = 100, unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RoleEnum role;

    @Builder.Default
    @Column(name = "reliability_score", nullable = false)
    private Integer reliabilityScore = 100;

    // Thông tin ngân hàng & VietQR của Host để nhận tiền chia sẻ P2P
    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "bank_account_no", length = 50)
    private String bankAccountNo;

    @Column(name = "vietqr_image_url", columnDefinition = "TEXT")
    private String vietqrImageUrl;

    @Builder.Default
    @Column(length = 20, nullable = false)
    private String status = "ACTIVE"; // ACTIVE | BANNED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}