package com.passthepaper.repository;

import com.passthepaper.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppealRepository extends JpaRepository<Appeal, UUID> {
    List<Appeal> findByUserOrderByCreatedAtDesc(User user);
    List<Appeal> findByStatusOrderByCreatedAtDesc(Appeal.AppealStatus status);

    @Transactional
    void deleteByUser(User user);
}
