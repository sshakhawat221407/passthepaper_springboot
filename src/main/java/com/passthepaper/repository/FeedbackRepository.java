package com.passthepaper.repository;

import com.passthepaper.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    List<Feedback> findByUserOrderByCreatedAtDesc(User user);
    List<Feedback> findByItemOrderByCreatedAtDesc(Resource item);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.user LEFT JOIN FETCH f.item ORDER BY f.createdAt DESC")
    List<Feedback> findAllForAdmin();

    @Transactional
    void deleteByUser(User user);

    @Transactional
    void deleteByItem(Resource item);
}
