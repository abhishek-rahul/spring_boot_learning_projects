package com.flashkart.catalog.infra;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository using Projections for optimized product listing.
 * Demonstrates how to use projections to fetch only required fields.
 */
@Repository
public interface ProductSummaryRepository extends JpaRepository<com.flashkart.catalog.domain.Product, UUID> {

    /**
     * JPQL query with projection to fetch product summary with min/max prices.
     * This avoids loading full Product entity and all its relationships.
     * Uses GROUP BY and aggregate functions to calculate price range.
     */
    @Query("SELECT p.id as id, " +
           "p.name as name, " +
           "p.description as description, " +
           "p.brand as brand, " +
           "p.imageUrl as imageUrl, " +
           "p.category.id as categoryId, " +
           "p.category.name as categoryName, " +
           "MIN(pr.amount) as minPrice, " +
           "MAX(pr.amount) as maxPrice, " +
           "p.active as active " +
           "FROM Product p " +
           "LEFT JOIN p.category " +
           "LEFT JOIN p.skus s " +
           "LEFT JOIN s.currentPrice pr " +
           "WHERE (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:brand IS NULL OR p.brand = :brand) " +
           "AND (:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND p.active = true AND p.deletedAt IS NULL " +
           "GROUP BY p.id, p.name, p.description, p.brand, p.imageUrl, p.category.id, p.category.name, p.active")
    Page<ProductProjection> findProductSummaries(
            @Param("categoryId") UUID categoryId,
            @Param("brand") String brand,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );
}
