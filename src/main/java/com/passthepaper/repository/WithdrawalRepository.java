package com.passthepaper.repository;

import com.passthepaper.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, UUID> {
    List<Withdrawal> findByUserOrderByCreatedAtDesc(User user);
    List<Withdrawal> findByStatusOrderByCreatedAtDesc(Withdrawal.WithdrawalStatus status);

    @Transactional
    void deleteByUser(User user);
}
