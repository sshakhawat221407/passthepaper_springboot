package com.passthepaper.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TxnType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TxnCurrency currency = TxnCurrency.BDT;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TxnStatus status = TxnStatus.pending;

    @Column(name = "points_topup_rate", precision = 10, scale = 2)
    private BigDecimal pointsTopupRate;

    @Column(name = "payment_phone", length = 50)
    private String paymentPhone;

    @Column(name = "transaction_number", length = 100)
    private String transactionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_plan")
    private User.MembershipPlan membershipPlan;

    @Column(name = "related_id")
    private UUID relatedId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum TxnType { add, purchase, upload_reward, withdrawal, topup_points, membership }
    public enum TxnCurrency { BDT, Points }
    public enum TxnStatus { pending, approved, rejected }
    public enum PaymentMethod { Bkash, Nagad, Card, Bank_Transfer, Wallet }
}
