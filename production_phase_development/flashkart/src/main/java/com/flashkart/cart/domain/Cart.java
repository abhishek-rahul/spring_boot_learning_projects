package com.flashkart.cart.domain;

import com.flashkart.shared.domain.BaseAuditEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cart entity representing a user's shopping cart.
 * Each user has one active cart at a time.
 * Carts expire after a configured period of inactivity.
 */
@Entity
@Table(name = "carts")
public class Cart extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {}

    public Cart(UUID userId, Instant expiresAt) {
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public List<CartItem> getItems() {
        return items;
    }

    /**
     * Check if the cart has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Extend the cart expiry time.
     */
    public void extendExpiry(Instant newExpiresAt) {
        this.expiresAt = newExpiresAt;
    }

    /**
     * Add an item to the cart.
     */
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    /**
     * Remove an item from the cart.
     */
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    /**
     * Get total number of items in the cart.
     */
    public int getTotalItemCount() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}

