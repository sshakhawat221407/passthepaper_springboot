package com.passthepaper.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appeals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_name", nullable = false, length = 150)
    private String userName;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppealStatus status = AppealStatus.pending;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public enum AppealStatus { pending, approved, rejected }
}
