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
    ORDER_NOT_CANCELLABLE,


    // identity domain
    USER_ALREADY_EXISTS,
    INVALID_CREDENTIALS,
    USER_NOT_FOUND,
    USER_BLOCKED,
    USER_NOT_ACTIVE,

    // security domain
    LOGIN_THROTTLED,
}
