package com.flashkart.catalog.infra;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projection interface for Product listing.
 * Used to select only required fields instead of full entity (performance optimization).
 * This avoids loading unnecessary data and reduces memory footprint.
 */
public interface ProductProjection {
    UUID getId();
    String getName();
    String getDescription();
    String getBrand();
    String getImageUrl();
    UUID getCategoryId();
    String getCategoryName();
    BigDecimal getMinPrice();
    BigDecimal getMaxPrice();
    boolean isActive();
}
