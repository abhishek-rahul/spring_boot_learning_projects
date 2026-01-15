package com.flashkart.order.domain;

import com.flashkart.shared.domain.BaseAuditEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Order item entity representing a line item in an order.
 */
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    protected OrderItem() {}

    public OrderItem(Order order, UUID skuId, Integer quantity, BigDecimal unitPrice, String currency) {
        this.order = order;
        this.skuId = skuId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.currency = currency;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public String getCurrency() {
        return currency;
    }
}
