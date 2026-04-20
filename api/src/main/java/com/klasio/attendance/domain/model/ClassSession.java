package com.klasio.attendance.domain.model;

import com.klasio.attendance.domain.event.SessionAlertRaised;
import com.klasio.attendance.domain.event.SessionAlertUpdated;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.attendance.domain.exception.AlertAuthorViolationException;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * ClassSession aggregate. Lazily materialized — one row per real interaction.
 * Pure Java domain model — zero Spring imports.
 */
public class ClassSession {

    private static final int MIN_REASON_LENGTH = 20;

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

    private final List<DomainEvent> domainEvents = new ArrayList<>();

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

    // ---------------------------------------------------------------
    // Validation helper
    // ---------------------------------------------------------------

    private static void validateReason(String reason) {
        Objects.requireNonNull(reason, "reason must not be null");
        if (reason.trim().length() < MIN_REASON_LENGTH) {
            throw new IllegalArgumentException(
                    "reason must be at least " + MIN_REASON_LENGTH + " characters");
        }
    }

    // ---------------------------------------------------------------
    // State transitions (RF-27 / RF-28)
    // ---------------------------------------------------------------

    /**
     * Raises an alert on this session (SCHEDULED or ALERTED → ALERTED).
     * Emits {@link SessionAlertRaised}.
     */
    public void raiseAlert(String reason, UUID alertedBy, String actorRole) {
        Objects.requireNonNull(alertedBy, "alertedBy must not be null");
        Objects.requireNonNull(actorRole, "actorRole must not be null");
        validateReason(reason);
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
        this.domainEvents.add(new SessionAlertRaised(
                this.id.value(), this.tenantId, this.classId, reason, alertedBy, actorRole, now));
    }

    /**
     * Updates the alert reason on an ALERTED session. Only the original author may update.
     * Emits {@link SessionAlertUpdated}.
     */
    public void updateAlertReason(String newReason, UUID actorId, String actorRole) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(actorRole, "actorRole must not be null");
        validateReason(newReason);
        if (this.status != ClassSessionStatus.ALERTED) {
            throw new IllegalStateException("Can only update alert reason on ALERTED sessions");
        }
        if (!actorId.equals(this.alertedBy)) {
            throw new AlertAuthorViolationException();
        }
        Instant now = Instant.now();
        this.alertReason = newReason;
        this.updatedAt = now;
        this.updatedBy = actorId;
        this.domainEvents.add(new SessionAlertUpdated(
                this.id.value(), this.tenantId, this.classId, newReason, actorId, actorRole, now));
    }

    /**
     * Cancels this session (any non-CANCELLED status → CANCELLED).
     * Emits {@link SessionCancelled}.
     */
    public void cancel(String reason, UUID cancelledBy, String actorRole) {
        Objects.requireNonNull(cancelledBy, "cancelledBy must not be null");
        Objects.requireNonNull(actorRole, "actorRole must not be null");
        validateReason(reason);
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
        // affectedStudentIds populated by CancelSessionService after fan-out; aggregate emits empty list
        this.domainEvents.add(new SessionCancelled(
                this.id.value(), this.tenantId, this.classId, reason, cancelledBy, actorRole,
                List.of(), now));
    }

    // ---------------------------------------------------------------
    // Domain event accessors
    // ---------------------------------------------------------------

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

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
