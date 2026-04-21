package com.klasio.auth.infrastructure.persistence;

import com.klasio.auth.domain.model.AccountSetupToken;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_setup_tokens")
public class AccountSetupTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountSetupTokenJpaEntity() {}

    public static AccountSetupTokenJpaEntity fromDomain(AccountSetupToken token) {
        AccountSetupTokenJpaEntity entity = new AccountSetupTokenJpaEntity();
        entity.id = token.getId();
        entity.userId = token.getUserId();
        entity.tokenHash = token.getTokenHash();
        entity.expiresAt = token.getExpiresAt();
        entity.used = token.isUsed();
        entity.createdAt = token.getCreatedAt();
        return entity;
    }

    public AccountSetupToken toDomain() {
        return new AccountSetupToken(id, userId, tokenHash, expiresAt, used, createdAt);
    }

    // Getters needed for invalidateAllForUser
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
