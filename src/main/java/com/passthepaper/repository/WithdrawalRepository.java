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
public interface WithdrawalRepository extends JpaRepository<Withdrawal, UUID> {
    List<Withdrawal> findByUserOrderByCreatedAtDesc(User user);
    List<Withdrawal> findByStatusOrderByCreatedAtDesc(Withdrawal.WithdrawalStatus status);
}
