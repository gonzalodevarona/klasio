package com.klasio.auth.infrastructure.persistence;

import com.klasio.auth.domain.model.RefreshToken;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "replaced_by_id")
    private UUID replacedById;

    protected RefreshTokenJpaEntity() {}

    public static RefreshTokenJpaEntity fromDomain(RefreshToken token) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.id = token.getId();
        entity.userId = token.getUserId();
        entity.tokenHash = token.getTokenHash();
        entity.tenantId = token.getTenantId();
        entity.issuedAt = token.getIssuedAt();
        entity.expiresAt = token.getExpiresAt();
        entity.revoked = token.isRevoked();
        entity.replacedById = token.getReplacedById();
        return entity;
    }

    public RefreshToken toDomain() {
        return new RefreshToken(id, userId, tokenHash, tenantId,
                issuedAt, expiresAt, revoked, replacedById);
    }
}
