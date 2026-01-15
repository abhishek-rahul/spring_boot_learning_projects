package com.flashkart.cart.domain;

import com.flashkart.shared.domain.BaseAuditEntity;
import jakarta.persistence.*;
import java.util.UUID;

/**
 * Cart item entity representing a single product SKU in a cart.
 * Each cart item represents a quantity of a specific SKU.
 */
@Entity
@Table(name = "cart_items")
public class CartItem extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(nullable = false)
    private Integer quantity;

    protected CartItem() {}

    public CartItem(Cart cart, UUID skuId, Integer quantity) {
        this.cart = cart;
        this.skuId = skuId;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public void setSkuId(UUID skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        this.quantity = quantity;
    }

    /**
     * Update the quantity of this cart item.
     */
    public void updateQuantity(Integer newQuantity) {
        setQuantity(newQuantity);
    }
}

