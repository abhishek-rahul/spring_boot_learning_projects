package com.flashkart.order.domain;

/**
 * Payment intent status enumeration.
 */
public enum PaymentIntentStatus {
    CREATED,    // Payment intent created
    PENDING,    // Payment pending
    SUCCEEDED,  // Payment successful
    FAILED,     // Payment failed
    CANCELLED   // Payment cancelled
}
