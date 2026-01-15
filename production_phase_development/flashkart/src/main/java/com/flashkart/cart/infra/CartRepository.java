package com.flashkart.cart.infra;

import com.flashkart.cart.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Cart entity.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    /**
     * Find the active cart for a user.
     * Active means not deleted and not expired.
     */
    @Query("SELECT c FROM Cart c " +
           "WHERE c.userId = :userId " +
           "AND c.deletedAt IS NULL " +
           "AND c.expiresAt > :now")
    Optional<Cart> findActiveCartByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Find all expired carts that haven't been deleted.
     */
    @Query("SELECT c FROM Cart c " +
           "WHERE c.deletedAt IS NULL " +
           "AND c.expiresAt <= :now")
    List<Cart> findExpiredCarts(@Param("now") Instant now);

    /**
     * Soft delete expired carts in batch.
     */
    @Modifying
    @Query("UPDATE Cart c SET c.deletedAt = :now " +
           "WHERE c.deletedAt IS NULL " +
           "AND c.expiresAt <= :expiryThreshold")
    int softDeleteExpiredCarts(@Param("now") Instant now, @Param("expiryThreshold") Instant expiryThreshold);

    /**
     * Find all carts for a user (including expired ones).
     */
    List<Cart> findByUserIdAndDeletedAtIsNull(UUID userId);
}

