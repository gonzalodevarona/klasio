package com.klasio.attendance.domain.model;

import com.klasio.attendance.domain.event.AttendanceCorrected;
import com.klasio.attendance.domain.event.AttendanceMarkedAbsent;
import com.klasio.attendance.domain.event.AttendanceMarkedPresent;
import com.klasio.attendance.domain.event.AttendanceMarkedPresentNoHours;
import com.klasio.attendance.domain.event.AttendanceRegistered;
import com.klasio.attendance.domain.event.RegistrationCancelled;
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
 * AttendanceRegistration aggregate root.
 * Models a student's intent to attend a specific class session.
 * Pure Java domain model — zero Spring imports.
 */
public class AttendanceRegistration {

    private final AttendanceRegistrationId id;
    private final UUID tenantId;
    private final UUID sessionId;
    private final UUID classId;
    private final UUID studentId;
    private final UUID enrollmentId;
    private final UUID membershipId;
    private final String levelAtRegistration;
    private final int intendedHours;
    private AttendanceRegistrationStatus status;

    // Snapshot fields for session context (denormalized for queries)
    private final LocalDate sessionDate;
    private final LocalTime sessionStartTime;
    private final LocalTime sessionEndTime;

    // Forward-compat (RF-24 / RF-25 / RF-28)
    private Instant cancelledAt;
    private UUID cancelledBy;
    private String cancellationReason;
    private Instant markedAt;
    private UUID markedBy;

    // Correction tracking (RF-26)
    private Instant correctedAt;
    private UUID correctedBy;
    private String correctionReason;

    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private AttendanceRegistration(AttendanceRegistrationId id, UUID tenantId,
                                   UUID sessionId, UUID classId, UUID studentId,
                                   UUID enrollmentId, UUID membershipId,
                                   String levelAtRegistration, int intendedHours,
                                   AttendanceRegistrationStatus status,
                                   LocalDate sessionDate, LocalTime sessionStartTime, LocalTime sessionEndTime,
                                   Instant cancelledAt, UUID cancelledBy, String cancellationReason,
                                   Instant markedAt, UUID markedBy,
                                   Instant correctedAt, UUID correctedBy, String correctionReason,
                                   Instant createdAt, UUID createdBy,
                                   Instant updatedAt, UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.classId = classId;
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.membershipId = membershipId;
        this.levelAtRegistration = levelAtRegistration;
        this.intendedHours = intendedHours;
        this.status = status;
        this.sessionDate = sessionDate;
        this.sessionStartTime = sessionStartTime;
        this.sessionEndTime = sessionEndTime;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.cancellationReason = cancellationReason;
        this.markedAt = markedAt;
        this.markedBy = markedBy;
        this.correctedAt = correctedAt;
        this.correctedBy = correctedBy;
        this.correctionReason = correctionReason;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    /**
     * Factory: registers a student for a session.
     *
     * @param classDurationMinutes duration of the class in minutes; must be >= 60
     * @param intendedHours        hours the student intends to consume; must be 1..floor(duration/60)
     */
    public static AttendanceRegistration register(UUID sessionId, UUID tenantId, UUID classId,
                                                   UUID studentId, UUID enrollmentId, UUID membershipId,
                                                   String levelAtRegistration, int intendedHours,
                                                   int classDurationMinutes,
                                                   LocalDate sessionDate, LocalTime sessionStartTime,
                                                   LocalTime sessionEndTime, UUID actorId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(classId, "classId must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
        Objects.requireNonNull(membershipId, "membershipId must not be null");
        Objects.requireNonNull(levelAtRegistration, "levelAtRegistration must not be null");
        Objects.requireNonNull(sessionDate, "sessionDate must not be null");
        Objects.requireNonNull(sessionStartTime, "sessionStartTime must not be null");
        Objects.requireNonNull(sessionEndTime, "sessionEndTime must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");

        if (classDurationMinutes < 60) {
            throw new IllegalArgumentException(
                    "Class duration must be at least 60 minutes to allow registration, got: " + classDurationMinutes);
        }

        int maxIntendedHours = classDurationMinutes / 60;
        if (intendedHours < 1 || intendedHours > maxIntendedHours) {
            throw new IllegalArgumentException(
                    "intendedHours must be between 1 and %d (floor of %d min / 60), got: %d"
                            .formatted(maxIntendedHours, classDurationMinutes, intendedHours));
        }

        Instant now = Instant.now();
        AttendanceRegistrationId regId = AttendanceRegistrationId.generate();

        AttendanceRegistration reg = new AttendanceRegistration(
                regId, tenantId, sessionId, classId, studentId, enrollmentId, membershipId,
                levelAtRegistration, intendedHours, AttendanceRegistrationStatus.REGISTERED,
                sessionDate, sessionStartTime, sessionEndTime,
                null, null, null, null, null,
                null, null, null,
                now, actorId, null, null
        );

        reg.domainEvents.add(new AttendanceRegistered(
                regId.value(), sessionId, tenantId, classId, studentId, enrollmentId, membershipId,
                levelAtRegistration, intendedHours, sessionDate, sessionStartTime, sessionEndTime,
                actorId, now));

        return reg;
    }

    public static AttendanceRegistration reconstitute(AttendanceRegistrationId id, UUID tenantId,
                                                       UUID sessionId, UUID classId, UUID studentId,
                                                       UUID enrollmentId, UUID membershipId,
                                                       String levelAtRegistration, int intendedHours,
                                                       AttendanceRegistrationStatus status,
                                                       LocalDate sessionDate, LocalTime sessionStartTime,
                                                       LocalTime sessionEndTime,
                                                       Instant cancelledAt, UUID cancelledBy,
                                                       String cancellationReason,
                                                       Instant markedAt, UUID markedBy,
                                                       Instant correctedAt, UUID correctedBy,
                                                       String correctionReason,
                                                       Instant createdAt, UUID createdBy,
                                                       Instant updatedAt, UUID updatedBy) {
        return new AttendanceRegistration(id, tenantId, sessionId, classId, studentId,
                enrollmentId, membershipId, levelAtRegistration, intendedHours, status,
                sessionDate, sessionStartTime, sessionEndTime,
                cancelledAt, cancelledBy, cancellationReason,
                markedAt, markedBy,
                correctedAt, correctedBy, correctionReason,
                createdAt, createdBy, updatedAt, updatedBy);
    }

    /**
     * Transitions REGISTERED → PRESENT. Emits AttendanceMarkedPresent.
     * Does NOT enforce the time window — the application service is responsible.
     */
    public void markPresent(UUID actorId, Instant now) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (this.status != AttendanceRegistrationStatus.REGISTERED) {
            throw new IllegalStateException("Cannot mark present from status: " + this.status);
        }
        this.status = AttendanceRegistrationStatus.PRESENT;
        this.markedAt = now;
        this.markedBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
        this.domainEvents.add(new AttendanceMarkedPresent(
                this.id.value(), this.sessionId, this.tenantId, this.classId,
                this.studentId, this.membershipId, this.intendedHours,
                this.sessionDate, actorId, now));
    }

    /**
     * Transitions REGISTERED → PRESENT_NO_HOURS. Emits AttendanceMarkedPresentNoHours.
     * Used when the student is present but has no (or insufficient) hours on their membership.
     */
    public void markPresentNoHours(UUID actorId, Instant now) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (this.status != AttendanceRegistrationStatus.REGISTERED) {
            throw new IllegalStateException("Cannot mark present (no hours) from status: " + this.status);
        }
        this.status = AttendanceRegistrationStatus.PRESENT_NO_HOURS;
        this.markedAt = now;
        this.markedBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
        this.domainEvents.add(new AttendanceMarkedPresentNoHours(
                this.id.value(), this.sessionId, this.tenantId, this.classId,
                this.studentId, this.membershipId, this.intendedHours,
                this.sessionDate, actorId, now));
    }

    /**
     * Transitions REGISTERED → ABSENT. Emits AttendanceMarkedAbsent.
     */
    public void markAbsent(UUID actorId, Instant now) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (this.status != AttendanceRegistrationStatus.REGISTERED) {
            throw new IllegalStateException("Cannot mark absent from status: " + this.status);
        }
        this.status = AttendanceRegistrationStatus.ABSENT;
        this.markedAt = now;
        this.markedBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
        this.domainEvents.add(new AttendanceMarkedAbsent(
                this.id.value(), this.sessionId, this.tenantId, this.classId,
                this.studentId, this.sessionDate, actorId, now));
    }

    /**
     * Corrects PRESENT or PRESENT_NO_HOURS → ABSENT. Emits AttendanceCorrected.
     * Does NOT trigger a refund — the application service handles that.
     */
    public void correctToAbsent(UUID actorId, Instant now, String reason) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (this.status != AttendanceRegistrationStatus.PRESENT
                && this.status != AttendanceRegistrationStatus.PRESENT_NO_HOURS) {
            throw new IllegalStateException(
                    "Can only correct to ABSENT from PRESENT or PRESENT_NO_HOURS. Current: " + this.status);
        }
        String previous = this.status.name();
        this.status = AttendanceRegistrationStatus.ABSENT;
        this.correctedAt = now;
        this.correctedBy = actorId;
        this.correctionReason = reason;
        this.updatedAt = now;
        this.updatedBy = actorId;
        this.domainEvents.add(new AttendanceCorrected(
                this.id.value(), this.tenantId, this.classId, this.studentId,
                previous, "ABSENT", reason, actorId, now));
    }

    /**
     * Corrects ABSENT → PRESENT. Emits AttendanceCorrected.
     * Does NOT deduct hours — the application service handles that.
     */
    public void correctToPresent(UUID actorId, Instant now, String reason) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (this.status != AttendanceRegistrationStatus.ABSENT) {
            throw new IllegalStateException(
                    "Can only correct to PRESENT from ABSENT. Current: " + this.status);
        }
        this.status = AttendanceRegistrationStatus.PRESENT;
        this.correctedAt = now;
        this.correctedBy = actorId;
        this.correctionReason = reason;
        this.markedAt = now;
        this.markedBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
        this.domainEvents.add(new AttendanceCorrected(
                this.id.value(), this.tenantId, this.classId, this.studentId,
                "ABSENT", "PRESENT", reason, actorId, now));
    }

    /**
     * Corrects ABSENT → PRESENT_NO_HOURS. Emits AttendanceCorrected.
     */
    public void correctToPresentNoHours(UUID actorId, Instant now, String reason) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (this.status != AttendanceRegistrationStatus.ABSENT) {
            throw new IllegalStateException(
                    "Can only correct to PRESENT_NO_HOURS from ABSENT. Current: " + this.status);
        }
        this.status = AttendanceRegistrationStatus.PRESENT_NO_HOURS;
        this.correctedAt = now;
        this.correctedBy = actorId;
        this.correctionReason = reason;
        this.markedAt = now;
        this.markedBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
        this.domainEvents.add(new AttendanceCorrected(
                this.id.value(), this.tenantId, this.classId, this.studentId,
                "ABSENT", "PRESENT_NO_HOURS", reason, actorId, now));
    }

    /**
     * Transitions this registration to CANCELLED_BY_STUDENT.
     * Only valid from REGISTERED status — throws IllegalStateException otherwise.
     * Does NOT enforce the time cutoff — that is the application service's responsibility.
     */
    public void cancelByStudent(UUID actorId, Instant now) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (this.status != AttendanceRegistrationStatus.REGISTERED) {
            throw new IllegalStateException(
                    "Cannot cancel registration in status: " + this.status);
        }
        this.status = AttendanceRegistrationStatus.CANCELLED_BY_STUDENT;
        this.cancelledAt = now;
        this.cancelledBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
        this.domainEvents.add(new RegistrationCancelled(
                this.id.value(), this.sessionId, this.tenantId, this.classId,
                this.studentId, this.sessionDate, this.sessionStartTime,
                actorId, now));
    }

    /**
     * Transitions this registration to SESSION_CANCELLED when the session itself is cancelled.
     * Valid from REGISTERED, PRESENT, PRESENT_NO_HOURS, or ABSENT status.
     * Idempotent: no-op if already SESSION_CANCELLED.
     * Emits {@link com.klasio.attendance.domain.event.RegistrationCancelledBySession}.
     */
    public void cancelBySession(UUID actorId, Instant now) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (this.status == AttendanceRegistrationStatus.SESSION_CANCELLED) {
            return; // idempotent
        }
        if (this.status == AttendanceRegistrationStatus.CANCELLED_BY_STUDENT
                || this.status == AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM) {
            throw new IllegalStateException(
                    "Cannot session-cancel a registration already cancelled (" + this.status + ")");
        }
        AttendanceRegistrationStatus prior = this.status;
        this.status = AttendanceRegistrationStatus.SESSION_CANCELLED;
        this.cancelledAt = now;
        this.cancelledBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
        this.domainEvents.add(new com.klasio.attendance.domain.event.RegistrationCancelledBySession(
                this.id.value(), this.tenantId, this.sessionId, this.classId, this.studentId,
                prior, actorId, now));
    }

    /**
     * Transitions this registration to CANCELLED_BY_SYSTEM (e.g., schedule changed).
     * Only valid from REGISTERED status — throws IllegalStateException otherwise.
     */
    public void cancelBySystem(UUID actorId, Instant now) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (this.status != AttendanceRegistrationStatus.REGISTERED) {
            throw new IllegalStateException(
                    "Cannot system-cancel registration in status: " + this.status);
        }
        this.status = AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM;
        this.cancelledAt = now;
        this.cancelledBy = actorId;
        this.cancellationReason = "Class schedule changed";
        this.updatedAt = now;
        this.updatedBy = actorId;
        this.domainEvents.add(new RegistrationCancelled(
                this.id.value(), this.sessionId, this.tenantId, this.classId,
                this.studentId, this.sessionDate, this.sessionStartTime,
                actorId, now));
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public AttendanceRegistrationId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSessionId() { return sessionId; }
    public UUID getClassId() { return classId; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getMembershipId() { return membershipId; }
    public String getLevelAtRegistration() { return levelAtRegistration; }
    public int getIntendedHours() { return intendedHours; }
    public AttendanceRegistrationStatus getStatus() { return status; }
    public LocalDate getSessionDate() { return sessionDate; }
    public LocalTime getSessionStartTime() { return sessionStartTime; }
    public LocalTime getSessionEndTime() { return sessionEndTime; }
    public Instant getCancelledAt() { return cancelledAt; }
    public UUID getCancelledBy() { return cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public Instant getMarkedAt() { return markedAt; }
    public UUID getMarkedBy() { return markedBy; }
    public Instant getCorrectedAt() { return correctedAt; }
    public UUID getCorrectedBy() { return correctedBy; }
    public String getCorrectionReason() { return correctionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
}
