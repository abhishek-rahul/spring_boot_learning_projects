package com.flashkart.catalog.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Product response.
 */
public record ProductResponse(
        UUID id,
        String name,
        String description,
        String brand,
        String imageUrl,
        CategoryResponse category,
        List<SkuResponse> skus,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
