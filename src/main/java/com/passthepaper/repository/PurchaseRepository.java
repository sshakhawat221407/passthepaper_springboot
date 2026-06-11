package com.passthepaper.repository;

import com.passthepaper.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    List<Purchase> findByUserOrderByPurchasedAtDesc(User user);
    boolean existsByUserAndResource(User user, Resource resource);
    List<Purchase> findByResourceOrderByPurchasedAtDesc(Resource resource);

    @Query("SELECT AVG(p.rating) FROM Purchase p WHERE p.resource.uploadedBy = :uploader AND p.rating IS NOT NULL")
    Double avgRatingByUploader(@Param("uploader") User uploader);

    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.resource.uploadedBy = :uploader AND p.rating IS NOT NULL")
    Long countRatingsByUploader(@Param("uploader") User uploader);

    @Transactional
    void deleteByUser(User user);

    @Transactional
    void deleteByResource(Resource resource);
}
