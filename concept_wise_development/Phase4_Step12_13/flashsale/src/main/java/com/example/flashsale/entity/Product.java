package com.example.flashsale.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    // Step 13: optimistic locking ready
    @Version
    private Long version;
}
