package com.passthepaper.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String university;

    @Column(name = "student_id", length = 100)
    private String studentId;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_admin", nullable = false)
    @Builder.Default
    private Boolean isAdmin = false;

    @Column(name = "is_banned", nullable = false)
    @Builder.Default
    private Boolean isBanned = false;

    @Column(name = "ban_reason")
    private String banReason;

    @Column(name = "wallet_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Column(name = "pending_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(name = "reward_points", nullable = false)
    @Builder.Default
    private Integer rewardPoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false)
    @Builder.Default
    private MembershipPlan membershipType = MembershipPlan.free;

    @Column(name = "membership_expiry")
    private Instant membershipExpiry;

    @Column(name = "profile_picture", columnDefinition = "MEDIUMTEXT")
    private String profilePicture;

    @Column(name = "id_card_image", columnDefinition = "MEDIUMTEXT")
    private String idCardImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_card_status", nullable = false)
    @Builder.Default
    private IdCardStatus idCardStatus = IdCardStatus.none;

    @Column(name = "seller_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal sellerRating = BigDecimal.ZERO;

    @Column(name = "total_ratings", nullable = false)
    @Builder.Default
    private Integer totalRatings = 0;

    // User restrictions
    @Column(name = "can_upload", nullable = false)
    @Builder.Default
    private Boolean canUpload = true;

    @Column(name = "can_purchase", nullable = false)
    @Builder.Default
    private Boolean canPurchase = true;

    @Column(name = "can_comment", nullable = false)
    @Builder.Default
    private Boolean canComment = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Relationships
    @OneToMany(mappedBy = "uploadedBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Resource> resources;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications;

    // Enums
    public enum MembershipPlan { free, premium_monthly, premium_yearly }
    public enum IdCardStatus { none, pending, approved, rejected }
}
