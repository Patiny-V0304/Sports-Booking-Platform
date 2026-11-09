package com.sportsplatform.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "transaction_type", length = 20, nullable = false)
    private String transactionType; // DEPOSIT_PAYMENT | REFUND

    @Column(name = "gateway_name", length = 20, nullable = false)
    private String gatewayName; // VNPAY | MOMO

    @Column(name = "gateway_txn_ref", length = 100, unique = true, nullable = false)
    private String gatewayTxnRef;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 20, nullable = false)
    private String status; // PENDING | SUCCESS | FAILED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}