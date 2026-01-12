package com.flashkart.catalog.api.dto;

import jakarta.validation.constraints.Min;
import java.util.UUID;

/**
 * DTO for product search/filter request.
 */
public record ProductSearchRequest(
        UUID categoryId,
        String brand,
        String searchTerm,
        @Min(0) Integer page,
        @Min(1) Integer size,
        String sortBy,
        String sortDirection
) {
    public ProductSearchRequest {
        // Default values
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sortBy == null) sortBy = "createdAt";
        if (sortDirection == null) sortDirection = "DESC";
    }
}
