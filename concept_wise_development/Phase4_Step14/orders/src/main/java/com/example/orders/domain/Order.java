package com.example.orders.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 32)
    private String status; // CREATED, PAID, FAILED

    @Column(name = "total_paise", nullable = false)
    private long totalPaise;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getTotalPaise() { return totalPaise; }
    public void setTotalPaise(long totalPaise) { this.totalPaise = totalPaise; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
