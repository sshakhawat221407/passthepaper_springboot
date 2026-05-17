package com.passthepaper.dto;

import com.passthepaper.entity.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class NotificationDto {
    @Builder
    public record Response(
        UUID id,
        String type,
        String title,
        String message,
        boolean isRead,
        UUID relatedId,
        Instant createdAt
    ) {}
}

// ─────────────── Feedback ───────────────
