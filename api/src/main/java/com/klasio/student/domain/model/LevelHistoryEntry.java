package com.klasio.student.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class LevelHistoryEntry {

    public enum Action { ENROLLED, PROMOTED, UNENROLLED }

    private final UUID id;
    private final UUID tenantId;
    private final UUID enrollmentId;
    private final Level previousLevel;
    private final Level newLevel;
    private final Action action;
    private final UUID changedBy;
    private final String changedByRole;
    private final Instant changedAt;
    private final String justification;

    private LevelHistoryEntry(UUID id,
                              UUID tenantId,
                              UUID enrollmentId,
                              Level previousLevel,
                              Level newLevel,
                              Action action,
                              UUID changedBy,
                              String changedByRole,
                              Instant changedAt,
                              String justification) {
        this.id = id;
        this.tenantId = tenantId;
        this.enrollmentId = enrollmentId;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
        this.action = action;
        this.changedBy = changedBy;
        this.changedByRole = changedByRole;
        this.changedAt = changedAt;
        this.justification = justification;
    }

    public static LevelHistoryEntry createInitial(UUID tenantId,
                                                   UUID enrollmentId,
                                                   Level newLevel,
                                                   UUID changedBy,
                                                   String changedByRole) {
        Objects.requireNonNull(newLevel, "New level must not be null");
        Objects.requireNonNull(changedBy, "Changed by must not be null");

        return new LevelHistoryEntry(
                UUID.randomUUID(),
                tenantId,
                enrollmentId,
                null,
                newLevel,
                Action.ENROLLED,
                changedBy,
                changedByRole,
                Instant.now(),
                null
        );
    }

    public static LevelHistoryEntry createPromotion(UUID tenantId,
                                                     UUID enrollmentId,
                                                     Level previousLevel,
                                                     Level newLevel,
                                                     UUID changedBy,
                                                     String changedByRole) {
        Objects.requireNonNull(previousLevel, "Previous level must not be null");
        Objects.requireNonNull(newLevel, "New level must not be null");
        Objects.requireNonNull(changedBy, "Changed by must not be null");

        return new LevelHistoryEntry(
                UUID.randomUUID(),
                tenantId,
                enrollmentId,
                previousLevel,
                newLevel,
                Action.PROMOTED,
                changedBy,
                changedByRole,
                Instant.now(),
                null
        );
    }

    public static LevelHistoryEntry createUnenrollment(UUID tenantId,
                                                        UUID enrollmentId,
                                                        Level previousLevel,
                                                        UUID changedBy,
                                                        String changedByRole) {
        Objects.requireNonNull(previousLevel, "Previous level must not be null");
        Objects.requireNonNull(changedBy, "Changed by must not be null");

        return new LevelHistoryEntry(
                UUID.randomUUID(),
                tenantId,
                enrollmentId,
                previousLevel,
                null,
                Action.UNENROLLED,
                changedBy,
                changedByRole,
                Instant.now(),
                null
        );
    }

    public static LevelHistoryEntry reconstitute(UUID id,
                                                  UUID tenantId,
                                                  UUID enrollmentId,
                                                  Level previousLevel,
                                                  Level newLevel,
                                                  Action action,
                                                  UUID changedBy,
                                                  String changedByRole,
                                                  Instant changedAt,
                                                  String justification) {
        return new LevelHistoryEntry(id, tenantId, enrollmentId, previousLevel,
                newLevel, action, changedBy, changedByRole, changedAt, justification);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public Level getPreviousLevel() { return previousLevel; }
    public Level getNewLevel() { return newLevel; }
    public Action getAction() { return action; }
    public UUID getChangedBy() { return changedBy; }
    public String getChangedByRole() { return changedByRole; }
    public Instant getChangedAt() { return changedAt; }
    public String getJustification() { return justification; }
}
