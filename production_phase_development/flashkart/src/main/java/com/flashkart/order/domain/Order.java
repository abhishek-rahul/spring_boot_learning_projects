package com.flashkart.order.domain;

import com.flashkart.shared.domain.BaseAuditEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order entity representing a customer order.
 * Orders go through a state machine: DRAFT -> PENDING_PAYMENT -> PAID -> PROCESSING -> SHIPPED -> DELIVERED
 */
@Entity
@Table(name = "orders")
public class Order extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "cart_id")
    private UUID cartId; // Reference to the cart that was checked out

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PaymentIntent paymentIntent;

    protected Order() {}

    public Order(UUID userId, String orderNumber, BigDecimal totalAmount, String currency, UUID cartId) {
        this.userId = userId;
        this.orderNumber = orderNumber;
        this.status = OrderStatus.DRAFT;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.cartId = cartId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public UUID getCartId() {
        return cartId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public PaymentIntent getPaymentIntent() {
        return paymentIntent;
    }

    public void setPaymentIntent(PaymentIntent paymentIntent) {
        this.paymentIntent = paymentIntent;
        if (paymentIntent != null) {
            paymentIntent.setOrder(this);
        }
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Check if order can transition to the given status.
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this.status) {
            case DRAFT -> newStatus == OrderStatus.PENDING_PAYMENT || newStatus == OrderStatus.CANCELLED;
            case PENDING_PAYMENT -> newStatus == OrderStatus.PAID || newStatus == OrderStatus.PAYMENT_FAILED || newStatus == OrderStatus.CANCELLED;
            case PAYMENT_FAILED -> newStatus == OrderStatus.PENDING_PAYMENT || newStatus == OrderStatus.CANCELLED;
            case PAID -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    /**
     * Transition order to new status if valid.
     */
    public void transitionTo(OrderStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition order from %s to %s", this.status, newStatus)
            );
        }
        this.status = newStatus;
    }
}
