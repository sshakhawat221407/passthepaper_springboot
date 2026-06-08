package com.passthepaper.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resources")
@NoArgsConstructor @AllArgsConstructor @Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", nullable = false)
    @Builder.Default
    private PriceType priceType = PriceType.money;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploader_name", nullable = false, length = 150)
    private String uploaderName;

    @Column(nullable = false)
    @Builder.Default
    private Integer downloads = 0;

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ResourceStatus status = ResourceStatus.pending;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(length = 150)
    private String department;

    @Column(length = 100)
    private String course;

    @Column(length = 100)
    private String semester;

    /** null = unlimited; when downloads >= maxSales the resource is sold out */
    @Column(name = "max_sales")
    private Integer maxSales;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum PriceType { money, points }
    public enum ResourceStatus { pending, approved, rejected }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public PriceType getPriceType() { return priceType; }
    public User getUploadedBy() { return uploadedBy; }
    public String getUploaderName() { return uploaderName; }
    public Integer getDownloads() { return downloads; }
    public BigDecimal getRating() { return rating; }
    public ResourceStatus getStatus() { return status; }
    public String getFileUrl() { return fileUrl; }
    public String getDepartment() { return department; }
    public String getCourse() { return course; }
    public String getSemester() { return semester; }
    public Integer getMaxSales() { return maxSales; }
    public boolean isSoldOut() { return maxSales != null && downloads >= maxSales; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setPriceType(PriceType priceType) { this.priceType = priceType; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
    public void setDownloads(Integer downloads) { this.downloads = downloads; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public void setStatus(ResourceStatus status) { this.status = status; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setDepartment(String department) { this.department = department; }
    public void setCourse(String course) { this.course = course; }
    public void setSemester(String semester) { this.semester = semester; }
    public void setMaxSales(Integer maxSales) { this.maxSales = maxSales; }
}
