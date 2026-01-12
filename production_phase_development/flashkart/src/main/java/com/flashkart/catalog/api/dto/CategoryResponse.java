package com.flashkart.catalog.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Category response.
 */
public record CategoryResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
