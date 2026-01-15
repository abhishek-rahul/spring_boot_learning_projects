package com.flashkart.order.domain;

import com.flashkart.shared.domain.BaseAuditEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Inventory reservation entity tracking reserved stock during checkout.
 */
@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InventoryReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected InventoryReservation() {}

    public InventoryReservation(UUID orderId, UUID skuId, Integer quantity, Instant expiresAt) {
        this.orderId = orderId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.status = InventoryReservationStatus.RESERVED;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public InventoryReservationStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryReservationStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
