package com.flashkart.catalog.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Price response.
 */
public record PriceResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        Integer version,
        boolean isCurrent,
        Instant createdAt
) {
}
