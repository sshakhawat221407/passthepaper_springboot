package com.passthepaper.dto;

import com.passthepaper.entity.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AdminDto {
    public record ApproveResourceRequest(UUID resourceId, boolean approve) {}
    public record ApproveTransactionRequest(UUID transactionId, boolean approve) {}
    public record ApproveWithdrawalRequest(UUID withdrawalId, boolean approve) {}
    public record ApproveIdCardRequest(UUID userId, boolean approve) {}
    public record MembershipRequest(UUID userId, String plan) {}
    public record BanRequest(String reason) {}
}

// ─────────────── Generic ───────────────
