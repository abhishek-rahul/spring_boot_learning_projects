package com.flashkart.catalog.infra;

import com.flashkart.catalog.domain.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specification for Product entity.
 * Demonstrates dynamic query building for complex filtering scenarios.
 * This pattern is useful when you need to build queries dynamically based on multiple optional filters.
 */
public class ProductSpecification {

    /**
     * Creates a specification for filtering products by multiple criteria.
     * This demonstrates the Specification pattern for complex dynamic queries.
     */
    public static Specification<Product> hasFilters(
            UUID categoryId,
            String brand,
            String searchTerm,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean active) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Soft delete filter (always applied)
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Category filter
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // Brand filter
            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.toLowerCase()));
            }

            // Search term filter (name contains)
            if (searchTerm != null && !searchTerm.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + searchTerm.toLowerCase() + "%"
                ));
            }

            // Active filter
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            // Price range filter (requires join with SKU and Price)
            if (minPrice != null || maxPrice != null) {
                var skuJoin = root.join("skus");
                var priceJoin = skuJoin.join("currentPrice");
                
                if (minPrice != null) {
                    predicates.add(cb.greaterThanOrEqualTo(priceJoin.get("amount"), minPrice));
                }
                if (maxPrice != null) {
                    predicates.add(cb.lessThanOrEqualTo(priceJoin.get("amount"), maxPrice));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a specification for active products only.
     */
    public static Specification<Product> isActive() {
        return (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("active"), true),
                cb.isNull(root.get("deletedAt"))
            );
    }

    /**
     * Creates a specification for products in a category.
     */
    public static Specification<Product> inCategory(UUID categoryId) {
        return (root, query, cb) -> 
            cb.equal(root.get("category").get("id"), categoryId);
    }
}
