package com.flashkart.cart.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for updating cart item quantity request.
 */
public record UpdateItemQuantityRequest(
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        Integer quantity
) {
}

