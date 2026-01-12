package com.flashkart.catalog.infra;

import com.flashkart.catalog.domain.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Price entity (price versioning).
 */
@Repository
public interface PriceRepository extends JpaRepository<Price, UUID> {

    // Derived query: find current price for SKU
    Optional<Price> findBySkuIdAndIsCurrentTrueAndDeletedAtIsNull(UUID skuId);

    // Derived query: find all prices for SKU (price history)
    List<Price> findBySkuIdAndDeletedAtIsNullOrderByVersionDesc(UUID skuId);

    // JPQL: find all current prices for multiple SKUs (bulk operation)
    @Query("SELECT p FROM Price p " +
           "WHERE p.sku.id IN :skuIds " +
           "AND p.isCurrent = true " +
           "AND p.deletedAt IS NULL")
    List<Price> findCurrentPricesBySkuIds(@Param("skuIds") List<UUID> skuIds);

    // JPQL: Mark all prices for a SKU as not current (before setting new current price)
    @Modifying
    @Query("UPDATE Price p SET p.isCurrent = false, p.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE p.sku.id = :skuId AND p.isCurrent = true AND p.deletedAt IS NULL")
    void markAllAsNotCurrent(@Param("skuId") UUID skuId);

    // Derived query: find latest version number for a SKU
    @Query("SELECT COALESCE(MAX(p.version), 0) FROM Price p " +
           "WHERE p.sku.id = :skuId AND p.deletedAt IS NULL")
    Integer findLatestVersionBySkuId(@Param("skuId") UUID skuId);
}
