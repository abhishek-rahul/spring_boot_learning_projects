package com.example.orders.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getProductId() { return productId; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public int getAvailableQty() { return availableQty; }
    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
