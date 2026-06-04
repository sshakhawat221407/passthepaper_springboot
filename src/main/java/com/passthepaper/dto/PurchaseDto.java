package com.passthepaper.dto;

import com.passthepaper.entity.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PurchaseDto {
   public record CheckoutRequest(
    @NotEmpty java.util.List<UUID> resourceIds,
    @NotBlank String paymentMethod,
    String paymentPhone,
    String transactionNumber
) {}

    public record RatingRequest(
        @NotNull UUID purchaseId,
        @NotNull @Min(1) @Max(5) short rating,
        String feedback
    ) {}

    @Builder
    public record Response(
        UUID id,
        UUID resourceId,
        String resourceTitle,
        BigDecimal price,
        String priceType,
        String paymentMethod,
        String feedback,
        Short rating,
        Instant purchasedAt
    ) {}
}

// ─────────────── Notification ───────────────
