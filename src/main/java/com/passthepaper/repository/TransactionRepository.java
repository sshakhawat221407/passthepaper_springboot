package com.passthepaper.repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  // ADD these two methods (replace the existing ones):

@Query("SELECT t FROM Transaction t JOIN FETCH t.user WHERE t.user = :user ORDER BY t.createdAt DESC")
List<Transaction> findByUserOrderByCreatedAtDesc(@Param("user") User user);

@Query("SELECT t FROM Transaction t JOIN FETCH t.user WHERE t.status = :status ORDER BY t.createdAt DESC")
List<Transaction> findByStatusOrderByCreatedAtDesc(@Param("status") Transaction.TxnStatus status);
}
