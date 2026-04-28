package com.klasio.membership.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable append-only entity. Never update or delete.
 */
public final class HourTransaction {

    private final HourTransactionId id;
    private final UUID tenantId;
    private final UUID membershipId;
    private final HourTransactionType type;
    private final int delta;
    private final String reason;
    private final UUID actorId;
    private final String actorRole;
    private final Instant createdAt;

    private HourTransaction(HourTransactionId id,
                             UUID tenantId,
                             UUID membershipId,
                             HourTransactionType type,
                             int delta,
                             String reason,
                             UUID actorId,
                             String actorRole,
                             Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.membershipId = membershipId;
        this.type = type;
        this.delta = delta;
        this.reason = reason;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.createdAt = createdAt;
    }

    public static HourTransaction create(UUID tenantId,
                                         UUID membershipId,
                                         HourTransactionType type,
                                         int delta,
                                         String reason,
                                         UUID actorId,
                                         String actorRole) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(membershipId, "membershipId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(actorRole, "actorRole must not be null");
        if (delta == 0) {
            throw new IllegalArgumentException("delta must not be zero");
        }

        return new HourTransaction(
                HourTransactionId.generate(),
                tenantId,
                membershipId,
                type,
                delta,
                reason,
                actorId,
                actorRole,
                Instant.now()
        );
    }

    /**
     * Creates an audit-only ledger row for UNLIMITED memberships.
     * Delta is always 0 — no balance change occurs; the row exists purely for traceability.
     */
    public static HourTransaction createForUnlimited(UUID tenantId,
                                                      UUID membershipId,
                                                      HourTransactionType type,
                                                      String reason,
                                                      UUID actorId,
                                                      String actorRole) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(membershipId, "membershipId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(actorRole, "actorRole must not be null");

        return new HourTransaction(
                HourTransactionId.generate(),
                tenantId,
                membershipId,
                type,
                0,
                reason,
                actorId,
                actorRole,
                Instant.now()
        );
    }

    public static HourTransaction reconstitute(HourTransactionId id,
                                                UUID tenantId,
                                                UUID membershipId,
                                                HourTransactionType type,
                                                int delta,
                                                String reason,
                                                UUID actorId,
                                                String actorRole,
                                                Instant createdAt) {
        return new HourTransaction(id, tenantId, membershipId, type, delta, reason,
                actorId, actorRole, createdAt);
    }

    public HourTransactionId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getMembershipId() { return membershipId; }
    public HourTransactionType getType() { return type; }
    public int getDelta() { return delta; }
    public String getReason() { return reason; }
    public UUID getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public Instant getCreatedAt() { return createdAt; }
}
