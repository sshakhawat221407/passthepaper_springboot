package com.passthepaper.dto;

import com.passthepaper.entity.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class FeedbackDto {
    public record CreateRequest(
        @NotBlank String type,
        @NotNull @Min(1) @Max(5) short rating,
        @NotBlank String comment,
        UUID itemId,
        String itemTitle
    ) {}
}

// ─────────────── Appeal ───────────────
