package com.flashkart.catalog.infra;

import com.flashkart.catalog.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Product entity.
 * Demonstrates multiple query patterns: derived queries, JPQL, native queries, and projections.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    // ========== Derived Query Methods ==========
    
    // Find active products (soft delete aware)
    Page<Product> findByActiveTrueAndDeletedAtIsNull(Pageable pageable);

    // Find by category (derived query)
    Page<Product> findByCategoryIdAndActiveTrueAndDeletedAtIsNull(UUID categoryId, Pageable pageable);

    // Find by brand (derived query)
    Page<Product> findByBrandAndActiveTrueAndDeletedAtIsNull(String brand, Pageable pageable);

    // Find by name containing (case-insensitive)
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String name, Pageable pageable);

    // ========== JPQL Queries ==========

    /**
     * JPQL query to find products with category loaded (avoiding N+1 problem).
     * Uses JOIN FETCH to eagerly load category in a single query.
     */
    @Query("SELECT p FROM Product p " +
           "JOIN FETCH p.category " +
           "WHERE p.active = true AND p.deletedAt IS NULL " +
           "ORDER BY p.createdAt DESC")
    List<Product> findActiveProductsWithCategory();

    /**
     * JPQL query for search with category filter.
     * Uses JOIN FETCH to avoid N+1 queries when accessing category.
     */
    @Query("SELECT p FROM Product p " +
           "JOIN FETCH p.category " +
           "WHERE (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:brand IS NULL OR p.brand = :brand) " +
           "AND (:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND p.active = true AND p.deletedAt IS NULL")
    Page<Product> searchProducts(
            @Param("categoryId") UUID categoryId,
            @Param("brand") String brand,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * JPQL query to find product by ID with category (for detail view).
     * Uses JOIN FETCH to load category eagerly and avoid N+1.
     */
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdWithCategory(@Param("id") UUID id);

    // ========== Native Query (for complex SQL) ==========

    /**
     * Native SQL query for full-text search using PostgreSQL tsvector.
     * Useful for complex search scenarios that are difficult with JPQL.
     */
    @Query(value = "SELECT p.* FROM products p " +
                   "WHERE p.deleted_at IS NULL " +
                   "AND p.active = true " +
                   "AND (:searchTerm IS NULL OR to_tsvector('english', p.name) @@ plainto_tsquery('english', :searchTerm)) " +
                   "ORDER BY ts_rank(to_tsvector('english', p.name), plainto_tsquery('english', :searchTerm)) DESC",
           nativeQuery = true)
    List<Product> fullTextSearch(@Param("searchTerm") String searchTerm, Pageable pageable);
}
