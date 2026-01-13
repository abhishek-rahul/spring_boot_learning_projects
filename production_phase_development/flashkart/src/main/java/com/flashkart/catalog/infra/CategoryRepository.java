package com.flashkart.catalog.infra;

import com.flashkart.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Category entity.
 * Uses derived query methods (Spring Data JPA).
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // Derived query method: find by name (case-insensitive)
    Optional<Category> findByNameIgnoreCase(String name);

    // Derived query method: find all active categories
    List<Category> findByActiveTrueAndDeletedAtIsNull();

    // Derived query method: check existence by name
    boolean existsByNameIgnoreCase(String name);
}
