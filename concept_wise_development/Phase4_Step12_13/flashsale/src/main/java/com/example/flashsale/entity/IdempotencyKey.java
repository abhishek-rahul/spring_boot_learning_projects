package com.example.flashsale.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency_keys",
       uniqueConstraints = @UniqueConstraint(name = "uk_idem_key", columnNames = {"idemKey"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class IdempotencyKey {

    public enum Status { IN_PROGRESS, COMPLETED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idemKey;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private Long orderId;

    @Column(nullable = false)
    private Instant createdAt;
}
