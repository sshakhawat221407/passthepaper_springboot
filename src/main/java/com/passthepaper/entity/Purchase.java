package com.passthepaper.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchases")
@NoArgsConstructor @AllArgsConstructor @Builder
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
    private Short rating;

    @CreationTimestamp
    @Column(name = "purchased_at", nullable = false, updatable = false)
    private Instant purchasedAt;

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Resource getResource() { return resource; }
    public BigDecimal getPrice() { return price; }
    public Resource.PriceType getPriceType() { return priceType; }
    public Transaction.PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getFeedback() { return feedback; }
    public Short getRating() { return rating; }
    public Instant getPurchasedAt() { return purchasedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setResource(Resource resource) { this.resource = resource; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setPriceType(Resource.PriceType priceType) { this.priceType = priceType; }
    public void setPaymentMethod(Transaction.PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public void setRating(Short rating) { this.rating = rating; }
}
