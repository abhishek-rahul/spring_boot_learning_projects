package com.flashkart.order.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for checkout operation.
 */
public record CheckoutRequest(
        @NotNull(message = "Cart ID is required")
        UUID cartId,
        
        String idempotencyKey // Optional idempotency key for safe retry
) {
}
