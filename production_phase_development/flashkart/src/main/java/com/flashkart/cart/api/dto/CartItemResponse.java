package com.flashkart.cart.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for cart item response.
 */
public record CartItemResponse(
        UUID id,
        UUID skuId,
        Integer quantity,
        Instant createdAt,
        Instant updatedAt
) {
}

