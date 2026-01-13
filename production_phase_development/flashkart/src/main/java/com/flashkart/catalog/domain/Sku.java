package com.flashkart.catalog.domain;

import com.flashkart.shared.domain.BaseAuditEntity;
import jakarta.persistence.*;
import java.util.UUID;

/**
 * SKU (Stock Keeping Unit) entity representing a product variant.
 * Each SKU has a current price and maintains price history through Price entity.
 */
@Entity
@Table(name = "skus")
public class Sku extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 50, unique = true)
    private String skuCode;

    @Column(length = 200)
    private String variantName; // e.g., "Red - Large", "Blue - Small"

    @Column(nullable = false)
    private Integer stockQuantity = 0;

    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
    @JoinColumn(name = "current_price_id")
    private Price currentPrice;

    @Column(nullable = false)
    private boolean active = true;

    protected Sku() {}

    public Sku(String skuCode, String variantName, Product product, Integer stockQuantity) {
        this.skuCode = skuCode;
        this.variantName = variantName;
        this.product = product;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
        this.reservedQuantity = 0;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getAvailableQuantity() {
        return stockQuantity - reservedQuantity;
    }

    public Price getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Price currentPrice) {
        this.currentPrice = currentPrice;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
