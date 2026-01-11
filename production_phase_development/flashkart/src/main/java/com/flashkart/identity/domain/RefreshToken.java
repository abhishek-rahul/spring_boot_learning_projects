package com.flashkart.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    private UUID replacedBy;

    private String createdByIp;
    private String userAgent;

    protected RefreshToken() {}

    public RefreshToken(UUID userId, String tokenHash, Instant issuedAt, Instant expiresAt,
                        String createdByIp, String userAgent) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.createdByIp = createdByIp;
        this.userAgent = userAgent;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }

    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isRevoked() { return revokedAt != null; }

    public void revoke(UUID replacedBy) {
        this.revokedAt = Instant.now();
        this.replacedBy = replacedBy;
    }
}
