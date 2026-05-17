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
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    List<Feedback> findByUserOrderByCreatedAtDesc(User user);
    List<Feedback> findByItemOrderByCreatedAtDesc(Resource item);

    @Query("SELECT f FROM Feedback f ORDER BY f.createdAt DESC")
    Page<Feedback> findAllPaged(Pageable pageable);
}
