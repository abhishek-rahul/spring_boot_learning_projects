package com.flashkart.order.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for order item.
 */
public record OrderItemResponse(
        UUID id,
        UUID skuId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String currency
) {
}
