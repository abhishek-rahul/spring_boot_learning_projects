package com.example.orders.api.dto;

public class CheckoutResponse {
    private Long orderId;
    private String status;
    private long totalPaise;

    public CheckoutResponse(Long orderId, String status, long totalPaise) {
        this.orderId = orderId;
        this.status = status;
        this.totalPaise = totalPaise;
    }

    public Long getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public long getTotalPaise() { return totalPaise; }
}
