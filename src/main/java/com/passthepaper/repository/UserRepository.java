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
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByIsAdminFalseAndIsBannedFalse();
    List<User> findByIsVerifiedFalseAndIsAdminFalse();
    List<User> findByIdCardStatus(User.IdCardStatus status);

    @Query("SELECT u FROM User u WHERE u.isAdmin = false ORDER BY u.createdAt DESC")
    Page<User> findAllStudents(Pageable pageable);
}
