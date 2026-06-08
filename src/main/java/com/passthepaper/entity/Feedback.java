package com.passthepaper.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feedbacks")
@NoArgsConstructor @AllArgsConstructor @Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "purchases", "cartItems", "notifications"})
private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FeedbackType type = FeedbackType.system;

    @Column(nullable = false)
    private Short rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "item_id")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "uploadedBy"})
private Resource item;

    @Column(name = "item_title", length = 255)
    private String itemTitle;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum FeedbackType { system, item }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public FeedbackType getType() { return type; }
    public Short getRating() { return rating; }
    public String getComment() { return comment; }
    public Resource getItem() { return item; }
    public String getItemTitle() { return itemTitle; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setType(FeedbackType type) { this.type = type; }
    public void setRating(Short rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setItem(Resource item) { this.item = item; }
    public void setItemTitle(String itemTitle) { this.itemTitle = itemTitle; }
}
