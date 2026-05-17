package com.passthepaper.dto;

import com.passthepaper.entity.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class WalletDto {
    public record WithdrawRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String method,
        @NotBlank String accountNumber
    ) {}
}

// ─────────────── Purchase ───────────────
