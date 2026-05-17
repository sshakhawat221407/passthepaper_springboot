package com.passthepaper.dto;

import com.passthepaper.entity.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionDto {

    public record AddFundsRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String paymentMethod,
        String paymentPhone,
        String transactionNumber
    ) {}

    public record TopupPointsRequest(
        @NotNull @Positive int points,
        @NotNull @Positive BigDecimal bdtCost
    ) {}

    @Builder
    public record Response(
        UUID id,
        UUID userId,
        String type,
        BigDecimal amount,
        String currency,
        String description,
        String paymentMethod,
        String status,
        BigDecimal pointsTopupRate,
        String membershipPlan,
        Instant createdAt
    ) {}
}

// ─────────────── Wallet ───────────────
