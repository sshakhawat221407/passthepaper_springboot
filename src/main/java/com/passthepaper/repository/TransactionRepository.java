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
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserOrderByCreatedAtDesc(User user);
    List<Transaction> findByStatusOrderByCreatedAtDesc(Transaction.TxnStatus status);
    List<Transaction> findByUserAndTypeOrderByCreatedAtDesc(User user, Transaction.TxnType type);

    @Query("SELECT t FROM Transaction t ORDER BY t.createdAt DESC")
    Page<Transaction> findAllPaged(Pageable pageable);
}
