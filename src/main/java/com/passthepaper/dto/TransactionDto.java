package com.passthepaper.dto;

import com.passthepaper.entity.*;
import jakarta.validation.constraints.*;
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

    public record Response(
        UUID id,
        String userId,
        String userName,
        String type,
        BigDecimal amount,
        String currency,
        String description,
        String paymentMethod,
        String status,
        String paymentPhone,
        String transactionNumber,
        BigDecimal pointsTopupRate,
        String membershipPlan,
        Instant createdAt
    ) {
        public static Response from(Transaction t) {
            return new Response(
                t.getId(),
                t.getUser() != null ? t.getUser().getId().toString() : null,
                t.getUser() != null ? t.getUser().getName() : null,
                t.getType().name(),
                t.getAmount(),
                t.getCurrency().name(),
                t.getDescription(),
                t.getPaymentMethod() != null ? t.getPaymentMethod().name() : null,
                t.getStatus().name(),
                t.getPaymentPhone(),
                t.getTransactionNumber(),
                t.getPointsTopupRate(),
                t.getMembershipPlan() != null ? t.getMembershipPlan().name() : null,
                t.getCreatedAt()
            );
        }
    }
}
