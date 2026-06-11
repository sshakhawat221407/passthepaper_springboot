package com.passthepaper.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.passthepaper.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserAndTypeOrderByCreatedAtDesc(User user, Transaction.TxnType type);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.user WHERE t.user = :user ORDER BY t.createdAt DESC")
    List<Transaction> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.user WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<Transaction> findByStatusOrderByCreatedAtDesc(@Param("status") Transaction.TxnStatus status);

    @Transactional
    void deleteByUser(User user);
}
