package com.flashkart.cart.infra;

import com.flashkart.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CartItem entity.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    /**
     * Find all items in a cart.
     */
    @Query("SELECT ci FROM CartItem ci " +
           "WHERE ci.cart.id = :cartId " +
           "AND ci.deletedAt IS NULL")
    List<CartItem> findByCartId(@Param("cartId") UUID cartId);

    /**
     * Find a specific cart item by cart ID and SKU ID.
     */
    @Query("SELECT ci FROM CartItem ci " +
           "WHERE ci.cart.id = :cartId " +
           "AND ci.skuId = :skuId " +
           "AND ci.deletedAt IS NULL")
    Optional<CartItem> findByCartIdAndSkuId(@Param("cartId") UUID cartId, @Param("skuId") UUID skuId);

    /**
     * Delete all items in a cart (soft delete).
     */
    @Query("SELECT ci FROM CartItem ci " +
           "WHERE ci.cart.id = :cartId " +
           "AND ci.deletedAt IS NULL")
    List<CartItem> findAllActiveByCartId(@Param("cartId") UUID cartId);
}

