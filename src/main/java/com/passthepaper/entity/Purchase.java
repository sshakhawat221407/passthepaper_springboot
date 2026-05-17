package com.passthepaper.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", nullable = false)
    private Resource.PriceType priceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private Transaction.PaymentMethod paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column
    private Short rating; // 1-5

    @CreationTimestamp
    @Column(name = "purchased_at", nullable = false, updatable = false)
    private Instant purchasedAt;
}
