package com.klasio.programclass.domain.model;

import com.klasio.programclass.domain.event.ClassCreated;
import com.klasio.programclass.domain.event.ClassDeactivated;
import com.klasio.programclass.domain.event.ClassReactivated;
import com.klasio.programclass.domain.event.ClassUpdated;
import com.klasio.programclass.domain.event.ProfessorAssignedToClass;
import com.klasio.programclass.domain.event.ProfessorRemovedFromClass;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ProgramClass {

    private final ProgramClassId id;
    private final UUID tenantId;
    private final UUID programId;
    private String name;
    private ClassLevel level;
    private final ClassType type;
    private UUID professorId;
    private int maxStudents;
    private ClassStatus status;
    private List<ClassScheduleEntry> scheduleEntries;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private ProgramClass(ProgramClassId id,
                         UUID tenantId,
                         UUID programId,
                         String name,
                         ClassLevel level,
                         ClassType type,
                         UUID professorId,
                         int maxStudents,
                         ClassStatus status,
                         List<ClassScheduleEntry> scheduleEntries,
                         Instant createdAt,
                         UUID createdBy,
                         Instant updatedAt,
                         UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.programId = programId;
        this.name = name;
        this.level = level;
        this.type = type;
        this.professorId = professorId;
        this.maxStudents = maxStudents;
        this.status = status;
        this.scheduleEntries = new ArrayList<>(scheduleEntries);
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static ProgramClass create(UUID tenantId,
                                      UUID programId,
                                      String name,
                                      ClassLevel level,
                                      ClassType type,
                                      List<ClassScheduleEntry> scheduleEntries,
                                      UUID professorId,
                                      int maxStudents,
                                      UUID createdBy) {
        Objects.requireNonNull(tenantId, "Tenant id must not be null");
        Objects.requireNonNull(programId, "Program id must not be null");
        Objects.requireNonNull(createdBy, "Created by must not be null");
        Objects.requireNonNull(level, "Level must not be null");
        Objects.requireNonNull(type, "Type must not be null");
        Objects.requireNonNull(scheduleEntries, "Schedule entries must not be null");
        validateNotBlank(name, "Name");
        validateNameLength(name);
        validateMaxStudents(maxStudents);
        validateScheduleEntries(scheduleEntries, type);

        Instant now = Instant.now();
        ProgramClassId id = ProgramClassId.generate();

        ProgramClass pc = new ProgramClass(
                id, tenantId, programId, name.trim(), level, type,
                professorId, maxStudents, ClassStatus.ACTIVE,
                scheduleEntries, now, createdBy, null, null);

        pc.domainEvents.add(new ClassCreated(
                id.value(), tenantId, programId, name.trim(),
                level.name(), type.name(), maxStudents, professorId,
                createdBy, now));

        return pc;
    }

    public static ProgramClass reconstitute(ProgramClassId id,
                                            UUID tenantId,
                                            UUID programId,
                                            String name,
                                            ClassLevel level,
                                            ClassType type,
                                            UUID professorId,
                                            int maxStudents,
                                            ClassStatus status,
                                            List<ClassScheduleEntry> scheduleEntries,
                                            Instant createdAt,
                                            UUID createdBy,
                                            Instant updatedAt,
                                            UUID updatedBy) {
        return new ProgramClass(id, tenantId, programId, name, level, type,
                professorId, maxStudents, status, scheduleEntries,
                createdAt, createdBy, updatedAt, updatedBy);
    }

    public void update(String name,
                       ClassLevel level,
                       List<ClassScheduleEntry> scheduleEntries,
                       int maxStudents,
                       UUID updatedBy) {
        Objects.requireNonNull(updatedBy, "Updated by must not be null");
        Objects.requireNonNull(level, "Level must not be null");
        Objects.requireNonNull(scheduleEntries, "Schedule entries must not be null");
        validateNotBlank(name, "Name");
        validateNameLength(name);
        validateMaxStudents(maxStudents);
        validateScheduleEntries(scheduleEntries, this.type);

        this.name = name.trim();
        this.level = level;
        this.scheduleEntries = new ArrayList<>(scheduleEntries);
        this.maxStudents = maxStudents;
        Instant now = Instant.now();
        this.updatedAt = now;
        this.updatedBy = updatedBy;

        domainEvents.add(new ClassUpdated(
                id.value(), tenantId, programId, this.name,
                level.name(), maxStudents, updatedBy, now));
    }

    public void deactivate(UUID deactivatedBy) {
        Objects.requireNonNull(deactivatedBy, "Deactivated by must not be null");
        if (this.status == ClassStatus.INACTIVE) {
            throw new IllegalStateException("Class is already inactive");
        }

        Instant now = Instant.now();
        this.status = ClassStatus.INACTIVE;
        this.updatedAt = now;
        this.updatedBy = deactivatedBy;

        domainEvents.add(new ClassDeactivated(id.value(), tenantId, programId, deactivatedBy, now));
    }

    public void reactivate(UUID reactivatedBy) {
        Objects.requireNonNull(reactivatedBy, "Reactivated by must not be null");
        if (this.status == ClassStatus.ACTIVE) {
            throw new IllegalStateException("Class is already active");
        }

        Instant now = Instant.now();
        this.status = ClassStatus.ACTIVE;
        this.updatedAt = now;
        this.updatedBy = reactivatedBy;

        domainEvents.add(new ClassReactivated(id.value(), tenantId, programId, reactivatedBy, now));
    }

    public void assignProfessor(UUID professorId, UUID assignedBy) {
        Objects.requireNonNull(professorId, "Professor id must not be null");
        Objects.requireNonNull(assignedBy, "Assigned by must not be null");

        Instant now = Instant.now();

        if (this.professorId != null) {
            domainEvents.add(new ProfessorRemovedFromClass(
                    id.value(), tenantId, programId, this.professorId, assignedBy, now));
        }

        this.professorId = professorId;
        this.updatedAt = now;
        this.updatedBy = assignedBy;

        domainEvents.add(new ProfessorAssignedToClass(
                id.value(), tenantId, programId, professorId, assignedBy, now));
    }

    public void removeProfessor(UUID removedBy) {
        Objects.requireNonNull(removedBy, "Removed by must not be null");
        if (this.professorId == null) {
            throw new IllegalStateException("No professor is assigned to this class");
        }

        Instant now = Instant.now();
        UUID previousProfessorId = this.professorId;
        this.professorId = null;
        this.updatedAt = now;
        this.updatedBy = removedBy;

        domainEvents.add(new ProfessorRemovedFromClass(
                id.value(), tenantId, programId, previousProfessorId, removedBy, now));
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public ProgramClassId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProgramId() { return programId; }
    public String getName() { return name; }
    public ClassLevel getLevel() { return level; }
    public ClassType getType() { return type; }
    public UUID getProfessorId() { return professorId; }
    public int getMaxStudents() { return maxStudents; }
    public ClassStatus getStatus() { return status; }
    public List<ClassScheduleEntry> getScheduleEntries() { return Collections.unmodifiableList(scheduleEntries); }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
        }
    }

    private static void validateNameLength(String name) {
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Name must not exceed 100 characters");
        }
    }

    private static void validateMaxStudents(int maxStudents) {
        if (maxStudents <= 0) {
            throw new IllegalArgumentException("Max students must be a positive integer");
        }
    }

    private static void validateScheduleEntries(List<ClassScheduleEntry> entries, ClassType type) {
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Schedule entries must not be empty");
        }

        if (type == ClassType.ONE_TIME) {
            if (entries.size() != 1) {
                throw new IllegalArgumentException("One-time class must have exactly one schedule entry");
            }
            ClassScheduleEntry entry = entries.get(0);
            if (entry.specificDate() == null) {
                throw new IllegalArgumentException("One-time class schedule entry must have a specific date");
            }
            if (!entry.specificDate().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("One-time class specific date must be in the future");
            }
        }

        if (type == ClassType.RECURRING) {
            for (ClassScheduleEntry entry : entries) {
                if (entry.dayOfWeek() == null) {
                    throw new IllegalArgumentException("Recurring class schedule entries must have dayOfWeek set");
                }
            }
        }
    }
}
