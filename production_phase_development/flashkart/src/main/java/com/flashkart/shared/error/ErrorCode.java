package com.flashkart.shared.error;

public enum ErrorCode {
    VALIDATION_ERROR,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    RATE_LIMITED,
    INTERNAL_ERROR,

    // domain-specific placeholders (future)
    INVENTORY_CONFLICT,
    PAYMENT_PROVIDER_DOWN,
    ORDER_NOT_CANCELLABLE
}
