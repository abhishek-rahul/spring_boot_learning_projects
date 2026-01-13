package com.flashkart.catalog.domain;

import com.flashkart.shared.domain.BaseAuditEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Product entity with soft delete support.
 * Products contain multiple SKUs (variants).
 */
@Entity
@Table(name = "products")
public class Product extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(length = 200)
    private String brand;

    @Column(length = 500)
    private String imageUrl;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Sku> skus = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    protected Product() {}

    public Product(String name, String description, Category category, String brand, String imageUrl) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<Sku> getSkus() {
        return skus;
    }

    public void addSku(Sku sku) {
        skus.add(sku);
        sku.setProduct(this);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
