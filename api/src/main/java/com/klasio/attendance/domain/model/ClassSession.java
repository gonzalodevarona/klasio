package com.klasio.attendance.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * ClassSession aggregate. Lazily materialized — one row per real interaction.
 * Pure Java domain model — zero Spring imports.
 */
public class ClassSession {

    private final ClassSessionId id;
    private final UUID tenantId;
    private final UUID classId;
    private final LocalDate sessionDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private int currentCapacity;
    private ClassSessionStatus status;

    // Forward-compat fields (RF-27 / RF-28)
    private String alertReason;
    private UUID alertedBy;
    private Instant alertedAt;
    private String cancellationReason;
    private UUID cancelledBy;
    private Instant cancelledAt;

    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    private ClassSession(ClassSessionId id, UUID tenantId, UUID classId,
                         LocalDate sessionDate, LocalTime startTime, LocalTime endTime,
                         int currentCapacity, ClassSessionStatus status,
                         String alertReason, UUID alertedBy, Instant alertedAt,
                         String cancellationReason, UUID cancelledBy, Instant cancelledAt,
                         Instant createdAt, UUID createdBy,
                         Instant updatedAt, UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.classId = classId;
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentCapacity = currentCapacity;
        this.status = status;
        this.alertReason = alertReason;
        this.alertedBy = alertedBy;
        this.alertedAt = alertedAt;
        this.cancellationReason = cancellationReason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static ClassSession materialize(UUID tenantId, UUID classId,
                                           LocalDate sessionDate, LocalTime startTime,
                                           LocalTime endTime, UUID actorId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(classId, "classId must not be null");
        Objects.requireNonNull(sessionDate, "sessionDate must not be null");
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        Instant now = Instant.now();
        return new ClassSession(
                ClassSessionId.generate(), tenantId, classId,
                sessionDate, startTime, endTime,
                0, ClassSessionStatus.SCHEDULED,
                null, null, null, null, null, null,
                now, actorId, null, null
        );
    }

    public static ClassSession reconstitute(ClassSessionId id, UUID tenantId, UUID classId,
                                             LocalDate sessionDate, LocalTime startTime, LocalTime endTime,
                                             int currentCapacity, ClassSessionStatus status,
                                             String alertReason, UUID alertedBy, Instant alertedAt,
                                             String cancellationReason, UUID cancelledBy, Instant cancelledAt,
                                             Instant createdAt, UUID createdBy,
                                             Instant updatedAt, UUID updatedBy) {
        return new ClassSession(id, tenantId, classId, sessionDate, startTime, endTime,
                currentCapacity, status,
                alertReason, alertedBy, alertedAt,
                cancellationReason, cancelledBy, cancelledAt,
                createdAt, createdBy, updatedAt, updatedBy);
    }

    // Forward-compat state transitions (RF-27 / RF-28 — not REST-exposed in RF-23)

    public void raiseAlert(String reason, UUID alertedBy) {
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(alertedBy, "alertedBy must not be null");
        if (this.status == ClassSessionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot alert a cancelled session");
        }
        Instant now = Instant.now();
        this.status = ClassSessionStatus.ALERTED;
        this.alertReason = reason;
        this.alertedBy = alertedBy;
        this.alertedAt = now;
        this.updatedAt = now;
        this.updatedBy = alertedBy;
    }

    public void cancel(String reason, UUID cancelledBy) {
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(cancelledBy, "cancelledBy must not be null");
        if (this.status == ClassSessionStatus.CANCELLED) {
            throw new IllegalStateException("Session is already cancelled");
        }
        Instant now = Instant.now();
        this.status = ClassSessionStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = now;
        this.updatedAt = now;
        this.updatedBy = cancelledBy;
    }

    public ClassSessionId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getClassId() { return classId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getCurrentCapacity() { return currentCapacity; }
    public ClassSessionStatus getStatus() { return status; }
    public String getAlertReason() { return alertReason; }
    public UUID getAlertedBy() { return alertedBy; }
    public Instant getAlertedAt() { return alertedAt; }
    public String getCancellationReason() { return cancellationReason; }
    public UUID getCancelledBy() { return cancelledBy; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
}
