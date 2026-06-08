package com.passthepaper.repository;

import com.passthepaper.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    List<Resource> findByStatusOrderByCreatedAtDesc(Resource.ResourceStatus status);

    List<Resource> findByStatusAndCategoryOrderByCreatedAtDesc(
            Resource.ResourceStatus status, String category);

    @Query("SELECT r FROM Resource r WHERE r.status = 'approved' AND (r.maxSales IS NULL OR r.downloads < r.maxSales) ORDER BY r.downloads DESC")
    List<Resource> findFeatured(Pageable pageable);

    List<Resource> findByUploadedByAndStatusOrderByCreatedAtDesc(
            User uploader, Resource.ResourceStatus status);

    List<Resource> findByUploadedByOrderByCreatedAtDesc(User uploader);

    @Query("SELECT r FROM Resource r WHERE r.status = 'approved' " +
           "AND (r.maxSales IS NULL OR r.downloads < r.maxSales) " +
           "AND (:category IS NULL OR r.category = :category) " +
           "AND (:search IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Resource> search(@Param("category") String category,
                          @Param("search") String search,
                          Pageable pageable);

    /** Admin: all resources regardless of status/sold-out */
    List<Resource> findAllByOrderByCreatedAtDesc();
}
