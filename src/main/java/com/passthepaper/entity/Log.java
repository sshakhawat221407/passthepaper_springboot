package com.passthepaper.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "logs")
@NoArgsConstructor @AllArgsConstructor @Builder
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogType type;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_name", length = 150)
    private String userName;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "target_user_name", length = 150)
    private String targetUserName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum LogType { user_action, admin_action, transaction, verification, system }

    public UUID getId() { return id; }
    public LogType getType() { return type; }
    public String getAction() { return action; }
    public String getDescription() { return description; }
    public UUID getUserId() { return userId; }
    public String getUserName() { return userName; }
    public UUID getTargetUserId() { return targetUserId; }
    public String getTargetUserName() { return targetUserName; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setType(LogType type) { this.type = type; }
    public void setAction(String action) { this.action = action; }
    public void setDescription(String description) { this.description = description; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setTargetUserId(UUID targetUserId) { this.targetUserId = targetUserId; }
    public void setTargetUserName(String targetUserName) { this.targetUserName = targetUserName; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
