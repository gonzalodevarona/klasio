package com.klasio.student.domain.model;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.student.domain.event.StudentEnrolled;
import com.klasio.student.domain.event.StudentPromoted;
import com.klasio.student.domain.event.StudentUnenrolled;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class StudentEnrollment {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final StudentEnrollmentId id;
    private final UUID tenantId;
    private final UUID studentId;
    private final UUID programId;
    private final Level level;
    private final LocalDate enrollmentDate;
    private String status;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private StudentEnrollment(StudentEnrollmentId id,
                              UUID tenantId,
                              UUID studentId,
                              UUID programId,
                              Level level,
                              LocalDate enrollmentDate,
                              String status,
                              Instant createdAt,
                              UUID createdBy,
                              Instant updatedAt,
                              UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.programId = programId;
        this.level = level;
        this.enrollmentDate = enrollmentDate;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static StudentEnrollment create(UUID tenantId,
                                           UUID studentId,
                                           UUID programId,
                                           Level level,
                                           UUID createdBy) {
        Objects.requireNonNull(tenantId, "Tenant id must not be null");
        Objects.requireNonNull(studentId, "Student id must not be null");
        Objects.requireNonNull(programId, "Program id must not be null");
        Objects.requireNonNull(level, "Level must not be null");
        Objects.requireNonNull(createdBy, "Created by must not be null");

        Instant now = Instant.now();
        StudentEnrollmentId id = StudentEnrollmentId.generate();
        LocalDate enrollmentDate = LocalDate.now();

        StudentEnrollment enrollment = new StudentEnrollment(
                id, tenantId, studentId, programId, level, enrollmentDate,
                STATUS_ACTIVE, now, createdBy, null, null
        );

        enrollment.domainEvents.add(new StudentEnrolled(
                id.value(), tenantId, studentId, programId, level.name(),
                createdBy, now));

        return enrollment;
    }

    public static StudentEnrollment reconstitute(StudentEnrollmentId id,
                                                 UUID tenantId,
                                                 UUID studentId,
                                                 UUID programId,
                                                 Level level,
                                                 LocalDate enrollmentDate,
                                                 String status,
                                                 Instant createdAt,
                                                 UUID createdBy,
                                                 Instant updatedAt,
                                                 UUID updatedBy) {
        return new StudentEnrollment(id, tenantId, studentId, programId, level,
                enrollmentDate, status, createdAt, createdBy, updatedAt, updatedBy);
    }

    public void deactivate(UUID deactivatedBy) {
        Objects.requireNonNull(deactivatedBy, "Deactivated by must not be null");
        if (STATUS_INACTIVE.equals(this.status)) {
            throw new IllegalStateException("Enrollment is already inactive");
        }

        this.status = STATUS_INACTIVE;
        this.updatedAt = Instant.now();
        this.updatedBy = deactivatedBy;
    }

    public void unenroll(UUID changedBy) {
        Objects.requireNonNull(changedBy, "Changed by must not be null");
        if (STATUS_INACTIVE.equals(this.status)) {
            throw new IllegalStateException("Enrollment is already inactive");
        }

        this.status = STATUS_INACTIVE;
        this.updatedAt = Instant.now();
        this.updatedBy = changedBy;

        domainEvents.add(new StudentUnenrolled(
                id.value(), tenantId, studentId, programId, level.name(),
                changedBy, Instant.now()));
    }

    public void deactivateForPromotion(UUID changedBy) {
        Objects.requireNonNull(changedBy, "Changed by must not be null");
        if (STATUS_INACTIVE.equals(this.status)) {
            throw new IllegalStateException("Enrollment is already inactive");
        }

        this.status = STATUS_INACTIVE;
        this.updatedAt = Instant.now();
        this.updatedBy = changedBy;

        domainEvents.add(new StudentPromoted(
                id.value(), tenantId, studentId, programId, level.name(), null,
                changedBy, Instant.now()));
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public StudentEnrollmentId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getStudentId() { return studentId; }
    public UUID getProgramId() { return programId; }
    public Level getLevel() { return level; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
}
