package com.passthepaper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "withdrawals")
@NoArgsConstructor @AllArgsConstructor @Builder
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transaction.PaymentMethod method;

    @Column(name = "account_number", nullable = false, length = 100)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WithdrawalStatus status = WithdrawalStatus.pending;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public enum WithdrawalStatus { pending, completed, rejected }

    public UUID getId()                             { return id; }
    @JsonIgnore
    public User getUser()                           { return user; }
    public BigDecimal getAmount()                   { return amount; }
    public Transaction.PaymentMethod getMethod()    { return method; }
    public String getAccountNumber()                { return accountNumber; }
    public WithdrawalStatus getStatus()             { return status; }
    public Instant getCreatedAt()                   { return createdAt; }
    public Instant getCompletedAt()                 { return completedAt; }

    /** Flat userId in JSON so frontend can filter without nesting. */
    @JsonProperty("userId")
    public String getUserId() {
        return user != null ? user.getId().toString() : null;
    }

    public void setId(UUID id)                                      { this.id = id; }
    public void setUser(User user)                                  { this.user = user; }
    public void setAmount(BigDecimal amount)                        { this.amount = amount; }
    public void setMethod(Transaction.PaymentMethod method)         { this.method = method; }
    public void setAccountNumber(String accountNumber)              { this.accountNumber = accountNumber; }
    public void setStatus(WithdrawalStatus status)                  { this.status = status; }
    public void setCompletedAt(Instant completedAt)                 { this.completedAt = completedAt; }
}
