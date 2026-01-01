package com.example.flashsale.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerOrder {

    public enum Status { CREATED, PAID, FAILED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Instant createdAt;
}
