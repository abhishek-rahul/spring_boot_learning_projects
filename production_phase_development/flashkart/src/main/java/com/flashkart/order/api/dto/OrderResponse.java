package com.flashkart.order.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for order.
 */
public record OrderResponse(
        UUID id,
        String orderNumber,
        String status,
        BigDecimal totalAmount,
        String currency,
        UUID cartId,
        List<OrderItemResponse> items,
        PaymentIntentResponse paymentIntent,
        Instant createdAt
) {
}
