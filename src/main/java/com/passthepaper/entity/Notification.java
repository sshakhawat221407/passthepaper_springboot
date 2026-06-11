package com.passthepaper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * @JsonIgnore prevents Jackson from trying to serialize the full User entity
     * (which would throw LazyInitializationException when open-in-view=false).
     * getUserId() below exposes only the UUID string as "userId" in the JSON.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationType type = NotificationType.system;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "related_id")
    private UUID relatedId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum NotificationType { purchase, sale, system, feedback }

    public UUID getId()             { return id; }
    @JsonIgnore                          // suppress "user" from appearing in JSON
    public User getUser()           { return user; }
    public NotificationType getType()   { return type; }
    public String getTitle()            { return title; }
    public String getMessage()          { return message; }
    public Boolean getIsRead()          { return isRead; }
    public UUID getRelatedId()          { return relatedId; }
    public Instant getCreatedAt()       { return createdAt; }

    /** Flat userId field in JSON so the frontend can filter without nesting. */
    @JsonProperty("userId")
    public String getUserId() {
        // Accessing the PK of a Hibernate proxy does NOT trigger lazy loading.
        return user != null ? user.getId().toString() : null;
    }

    public void setId(UUID id)                      { this.id = id; }
    public void setUser(User user)                  { this.user = user; }
    public void setType(NotificationType type)      { this.type = type; }
    public void setTitle(String title)              { this.title = title; }
    public void setMessage(String message)          { this.message = message; }
    public void setIsRead(Boolean isRead)           { this.isRead = isRead; }
    public void setRelatedId(UUID relatedId)        { this.relatedId = relatedId; }
}
