package com.klasio.auth.infrastructure.persistence;

import com.klasio.auth.domain.model.EmailVerificationToken;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationTokenJpaEntity {

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

    protected EmailVerificationTokenJpaEntity() {}

    public static EmailVerificationTokenJpaEntity fromDomain(EmailVerificationToken token) {
        EmailVerificationTokenJpaEntity entity = new EmailVerificationTokenJpaEntity();
        entity.id = token.getId();
        entity.userId = token.getUserId();
        entity.tokenHash = token.getTokenHash();
        entity.expiresAt = token.getExpiresAt();
        entity.used = token.isUsed();
        entity.createdAt = token.getCreatedAt();
        return entity;
    }

    public EmailVerificationToken toDomain() {
        return new EmailVerificationToken(id, userId, tokenHash, expiresAt, used, createdAt);
    }
}
