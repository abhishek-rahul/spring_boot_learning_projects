package com.example.flashsale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CheckoutRequest(
        @NotBlank String userId,
        @NotBlank String idempotencyKey,
        @NotEmpty @Valid List<CartItemRequest> items,

        // For learning Step 12 rollback behavior:
        boolean simulateRuntimeFailure,
        boolean simulateCheckedFailure
) {}
