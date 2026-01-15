package com.flashkart.catalog.infra;

import com.flashkart.catalog.domain.Sku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SKU entity.
 */
@Repository
public interface SkuRepository extends JpaRepository<Sku, UUID> {

    // Derived query: find by SKU code
    Optional<Sku> findBySkuCode(String skuCode);

    // Derived query: find all SKUs for a product
    List<Sku> findByProductIdAndActiveTrueAndDeletedAtIsNull(UUID productId);

    // JPQL: find SKU with product and price (eager loading to avoid N+1)
    @Query("SELECT s FROM Sku s " +
           "LEFT JOIN FETCH s.product p " +
           "LEFT JOIN FETCH p.category " +
           "LEFT JOIN FETCH s.currentPrice " +
           "WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Sku> findByIdWithRelations(@Param("id") UUID id);

    // JPQL: find all SKUs for a product with prices
    @Query("SELECT s FROM Sku s " +
           "LEFT JOIN FETCH s.currentPrice " +
           "WHERE s.product.id = :productId AND s.active = true AND s.deletedAt IS NULL")
    List<Sku> findByProductIdWithPrice(@Param("productId") UUID productId);

    // JPQL: find SKU with pessimistic lock for inventory reservation
    @Query("SELECT s FROM Sku s " +
           "WHERE s.id = :id AND s.deletedAt IS NULL")
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<Sku> findByIdForUpdate(@Param("id") UUID id);
}
