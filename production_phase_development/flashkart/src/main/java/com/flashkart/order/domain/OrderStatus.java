package com.flashkart.order.domain;

/**
 * Order status enumeration representing the lifecycle of an order.
 */
public enum OrderStatus {
    DRAFT,              // Order created but not yet submitted
    PENDING_PAYMENT,   // Order submitted, waiting for payment
    PAYMENT_FAILED,    // Payment attempt failed
    PAID,              // Payment successful
    PROCESSING,        // Order being prepared
    SHIPPED,           // Order shipped
    DELIVERED,         // Order delivered
    CANCELLED          // Order cancelled
}
