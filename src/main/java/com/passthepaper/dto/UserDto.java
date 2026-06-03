package com.passthepaper.dto;

import com.passthepaper.entity.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class UserDto {

    @Builder
    public record Response(
        UUID id,
        String email,
        String name,
        String university,
        String studentId,
        boolean isVerified,
        boolean isAdmin,
        boolean isBanned,
        String banReason,
        BigDecimal walletBalance,
        BigDecimal pendingBalance,
        int rewardPoints,
        String membershipType,
        Instant membershipExpiry,
        String profilePicture,
        String idCardImage,
        String idCardStatus,
        BigDecimal sellerRating,
        int totalRatings,
        boolean canUpload,
        boolean canPurchase,
        boolean canComment,
        Instant createdAt
    ) {
        public static Response from(User u) {
            return new Response(
                u.getId(), u.getEmail(), u.getName(), u.getUniversity(), u.getStudentId(),
                Boolean.TRUE.equals(u.getIsVerified()), Boolean.TRUE.equals(u.getIsAdmin()),
                Boolean.TRUE.equals(u.getIsBanned()), u.getBanReason(),
                u.getWalletBalance(), u.getPendingBalance(), u.getRewardPoints(),
                u.getMembershipType().name(), u.getMembershipExpiry(),
                u.getProfilePicture(), u.getIdCardImage(), u.getIdCardStatus().name(),
                u.getSellerRating(), u.getTotalRatings(),
                Boolean.TRUE.equals(u.getCanUpload()),
                Boolean.TRUE.equals(u.getCanPurchase()),
                Boolean.TRUE.equals(u.getCanComment()),
                u.getCreatedAt()
            );
        }
    }

    public record UpdateProfileRequest(String name, String university, String studentId, String profilePicture) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min=6) String newPassword) {}
    public record RestrictionsRequest(Boolean canUpload, Boolean canPurchase, Boolean canComment) {}
    public record BanRequest(@NotBlank String reason) {}
    public record UnbanRequest() {}
}

// ─────────────── Resource ───────────────
