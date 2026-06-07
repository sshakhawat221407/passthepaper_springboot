package com.passthepaper.dto;

import com.passthepaper.entity.*;
import jakarta.validation.constraints.*;
import lombok.*;
import com.passthepaper.entity.Appeal;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AppealDto {

    public record CreateRequest(@NotBlank String reason) {}

    public record ReviewRequest(boolean approve, String adminResponse) {}
}

// ─────────────── Admin ───────────────
    public record Response(
        UUID id,
        String userId,
        String userName,
        String userEmail,
        String reason,
        String status,
        String adminResponse,
        Instant createdAt,
        Instant reviewedAt
    ) {
        public static Response from(Appeal a) {
            return new Response(
                a.getId(),
                a.getUser().getId().toString(),
                a.getUserName(),
                a.getUserEmail(),
                a.getReason(),
                a.getStatus().name(),
                a.getAdminResponse(),
                a.getCreatedAt(),
                a.getReviewedAt()
            );
        }
    }
}
