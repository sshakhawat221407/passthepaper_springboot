package com.passthepaper.dto;

import com.passthepaper.entity.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ResourceDto {

    public record CreateRequest(
        @NotBlank String title,
        String description,
        @NotBlank String category,
        @NotNull BigDecimal price,
        @NotNull String priceType,
        String department,
        String course,
        String semester
    ) {}

    @Builder
    public record Response(
        UUID id,
        String title,
        String description,
        String category,
        BigDecimal price,
        String priceType,
        UUID uploadedBy,
        String uploaderName,
        int downloads,
        BigDecimal rating,
        String status,
        String fileUrl,
        String department,
        String course,
        String semester,
        Instant createdAt
    ) {
        public static Response from(Resource r) {
            return new Response(
                r.getId(), r.getTitle(), r.getDescription(), r.getCategory(),
                r.getPrice(), r.getPriceType().name(),
                r.getUploadedBy().getId(), r.getUploaderName(),
                r.getDownloads(), r.getRating(), r.getStatus().name(),
                r.getFileUrl(), r.getDepartment(), r.getCourse(), r.getSemester(),
                r.getCreatedAt()
            );
        }
    }
}

// ─────────────── Transaction ───────────────
