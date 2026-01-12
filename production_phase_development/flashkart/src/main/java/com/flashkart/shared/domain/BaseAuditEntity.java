package com.flashkart.shared.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Base entity with auditing fields and soft delete support.
 * Provides created_at, updated_at, and deleted_at fields.
 */
@MappedSuperclass
public abstract class BaseAuditEntity {

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = true)
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft delete the entity by setting deletedAt timestamp.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Restore a soft-deleted entity.
     */
    public void restore() {
        this.deletedAt = null;
        this.updatedAt = Instant.now();
    }
}
