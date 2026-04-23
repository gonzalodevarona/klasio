package com.klasio.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One-time token sent via email for unified account setup (password creation).
 * Expires in 15 minutes. Once used or expired, it cannot be reused.
 */
public class AccountSetupToken {

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private Instant expiresAt;
    private boolean used;
    private Instant createdAt;

    public AccountSetupToken(UUID id, UUID userId, String tokenHash,
                              Instant expiresAt, boolean used, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.used = used;
        this.createdAt = createdAt;
    }

    public static AccountSetupToken create(UUID userId, String tokenHash, Instant expiresAt) {
        return new AccountSetupToken(UUID.randomUUID(), userId, tokenHash,
                expiresAt, false, Instant.now());
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !used && !isExpired();
    }

    public void markUsed() {
        this.used = true;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public Instant getCreatedAt() { return createdAt; }
}
