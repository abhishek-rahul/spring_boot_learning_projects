package com.example.flashsale.dto;

import java.math.BigDecimal;

public record CheckoutResponse(
        Long orderId,
        String status,
        BigDecimal totalAmount
) {}
