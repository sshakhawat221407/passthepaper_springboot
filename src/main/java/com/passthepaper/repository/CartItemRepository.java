package com.passthepaper.repository;

import com.passthepaper.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndResource(User user, Resource resource);
    boolean existsByUserAndResource(User user, Resource resource);

    @Transactional
    void deleteByUserAndResource(User user, Resource resource);

    @Transactional
    void deleteByUser(User user);

    @Transactional
    void deleteByResource(Resource resource);
}
