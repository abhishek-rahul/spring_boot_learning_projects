package com.flashkart.catalog.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for SKU response.
 */
public record SkuResponse(
        UUID id,
        String skuCode,
        String variantName,
        Integer stockQuantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        PriceResponse currentPrice,
        boolean active,
        Instant createdAt
) {
}
