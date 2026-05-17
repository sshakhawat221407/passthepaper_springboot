package com.passthepaper.dto;

import com.passthepaper.entity.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AppealDto {
    public record CreateRequest(@NotBlank String reason) {}
    public record ReviewRequest(boolean approve, String adminResponse) {}
}

// ─────────────── Admin ───────────────
