package com.flashkart.order.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for payment intent.
 */
public record PaymentIntentResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        String status,
        String paymentProvider
) {
}
