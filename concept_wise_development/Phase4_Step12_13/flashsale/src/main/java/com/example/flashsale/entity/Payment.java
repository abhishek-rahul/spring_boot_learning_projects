package com.example.flashsale.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    public enum Status { PENDING, SUCCESS, FAILED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String gatewayRef;

    @Column(nullable = false)
    private Instant createdAt;
}
