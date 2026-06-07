package com.passthepaper.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor @AllArgsConstructor @Builder
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

    public enum MembershipPlan { free, premium_monthly, premium_yearly }
    public enum IdCardStatus { none, pending, approved, rejected }

    // Manual getters
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public String getUniversity() { return university; }
    public String getStudentId() { return studentId; }
    public Boolean getIsVerified() { return isVerified; }
    public Boolean getIsAdmin() { return isAdmin; }
    public Boolean getIsBanned() { return isBanned; }
    public String getBanReason() { return banReason; }
    public BigDecimal getWalletBalance() { return walletBalance; }
    public BigDecimal getPendingBalance() { return pendingBalance; }
    public Integer getRewardPoints() { return rewardPoints; }
    public MembershipPlan getMembershipType() { return membershipType; }
    public Instant getMembershipExpiry() { return membershipExpiry; }
    public String getProfilePicture() { return profilePicture; }
    public String getIdCardImage() { return idCardImage; }
    public IdCardStatus getIdCardStatus() { return idCardStatus; }
    public BigDecimal getSellerRating() { return sellerRating; }
    public Integer getTotalRatings() { return totalRatings; }
    public Boolean getCanUpload() { return canUpload; }
    public Boolean getCanPurchase() { return canPurchase; }
    public Boolean getCanComment() { return canComment; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Manual setters
    public void setId(UUID id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setName(String name) { this.name = name; }
    public void setUniversity(String university) { this.university = university; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }
    public void setIsBanned(Boolean isBanned) { this.isBanned = isBanned; }
    public void setBanReason(String banReason) { this.banReason = banReason; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }
    public void setPendingBalance(BigDecimal pendingBalance) { this.pendingBalance = pendingBalance; }
    public void setRewardPoints(Integer rewardPoints) { this.rewardPoints = rewardPoints; }
    public void setMembershipType(MembershipPlan membershipType) { this.membershipType = membershipType; }
    public void setMembershipExpiry(Instant membershipExpiry) { this.membershipExpiry = membershipExpiry; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    public void setIdCardImage(String idCardImage) { this.idCardImage = idCardImage; }
    public void setIdCardStatus(IdCardStatus idCardStatus) { this.idCardStatus = idCardStatus; }
    public void setSellerRating(BigDecimal sellerRating) { this.sellerRating = sellerRating; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }
    public void setCanUpload(Boolean canUpload) { this.canUpload = canUpload; }
    public void setCanPurchase(Boolean canPurchase) { this.canPurchase = canPurchase; }
    public void setCanComment(Boolean canComment) { this.canComment = canComment; }
}
