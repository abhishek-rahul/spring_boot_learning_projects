package com.flashkart.catalog.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Product summary (used in listing endpoints).
 * Contains minimal fields for performance optimization.
 */
public record ProductSummaryResponse(
        UUID id,
        String name,
        String description,
        String brand,
        String imageUrl,
        UUID categoryId,
        String categoryName,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean active
) {
}
