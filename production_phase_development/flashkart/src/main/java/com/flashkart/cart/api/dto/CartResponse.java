package com.flashkart.cart.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for cart response.
 */
public record CartResponse(
        UUID id,
        UUID userId,
        List<CartItemResponse> items,
        Integer totalItemCount,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}

