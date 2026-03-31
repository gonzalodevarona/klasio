package com.klasio.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken {

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private UUID tenantId;
    private Instant issuedAt;
    private Instant expiresAt;
    private boolean revoked;
    private UUID replacedById;

    public RefreshToken(UUID id, UUID userId, String tokenHash, UUID tenantId,
                        Instant issuedAt, Instant expiresAt, boolean revoked, UUID replacedById) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.tenantId = tenantId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.replacedById = replacedById;
    }

    public static RefreshToken create(UUID userId, String tokenHash, UUID tenantId, Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, tenantId,
                Instant.now(), expiresAt, false, null);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }

    public void replaceWith(UUID newTokenId) {
        this.revoked = true;
        this.replacedById = newTokenId;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public UUID getTenantId() { return tenantId; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public UUID getReplacedById() { return replacedById; }
}
