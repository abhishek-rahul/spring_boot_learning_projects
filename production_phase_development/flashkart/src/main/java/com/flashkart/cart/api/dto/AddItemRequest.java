package com.flashkart.cart.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/**
 * DTO for adding item to cart request.
 */
public record AddItemRequest(
        @NotNull(message = "SKU ID is required")
        UUID skuId,
        
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        Integer quantity
) {
}

