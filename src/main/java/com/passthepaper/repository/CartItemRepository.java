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
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndResource(User user, Resource resource);
    void deleteByUserAndResource(User user, Resource resource);
    void deleteByUser(User user);
    boolean existsByUserAndResource(User user, Resource resource);
}
