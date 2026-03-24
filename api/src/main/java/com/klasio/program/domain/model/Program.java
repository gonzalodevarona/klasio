package com.klasio.program.domain.model;

import com.klasio.program.domain.event.ProgramCreated;
import com.klasio.program.domain.event.ProgramDeactivated;
import com.klasio.program.domain.event.ProgramReactivated;
import com.klasio.program.domain.event.ProgramUpdated;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Program {

    private final ProgramId id;
    private final UUID tenantId;
    private String name;
    private ProgramStatus status;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Program(ProgramId id,
                    UUID tenantId,
                    String name,
                    ProgramStatus status,
                    Instant createdAt,
                    UUID createdBy,
                    Instant updatedAt,
                    UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static Program create(UUID tenantId,
                                 String name,
                                 UUID createdBy) {
        Objects.requireNonNull(tenantId, "Tenant id must not be null");
        Objects.requireNonNull(createdBy, "Created by must not be null");
        validateNotBlank(name, "Name");

        Instant now = Instant.now();
        ProgramId id = ProgramId.generate();

        Program program = new Program(
                id,
                tenantId,
                name,
                ProgramStatus.ACTIVE,
                now,
                createdBy,
                null,
                null
        );

        program.domainEvents.add(new ProgramCreated(
                id.value(),
                tenantId,
                name,
                createdBy,
                now
        ));

        return program;
    }

    public static Program reconstitute(ProgramId id,
                                       UUID tenantId,
                                       String name,
                                       ProgramStatus status,
                                       Instant createdAt,
                                       UUID createdBy,
                                       Instant updatedAt,
                                       UUID updatedBy) {
        return new Program(id, tenantId, name, status, createdAt, createdBy, updatedAt, updatedBy);
    }

    public void update(String name, UUID updatedBy) {
        Objects.requireNonNull(updatedBy, "Updated by must not be null");
        validateNotBlank(name, "Name");

        this.name = name;
        this.updatedAt = Instant.now();
        this.updatedBy = updatedBy;

        domainEvents.add(new ProgramUpdated(
                id.value(), tenantId, name, updatedBy, this.updatedAt));
    }

    public void deactivate(UUID deactivatedBy) {
        Objects.requireNonNull(deactivatedBy, "Deactivated by must not be null");
        if (this.status != ProgramStatus.ACTIVE) {
            throw new IllegalStateException("Program is already inactive");
        }

        Instant now = Instant.now();
        this.status = ProgramStatus.INACTIVE;
        this.updatedAt = now;
        this.updatedBy = deactivatedBy;

        domainEvents.add(new ProgramDeactivated(id.value(), tenantId, deactivatedBy, now));
    }

    public void reactivate(UUID reactivatedBy) {
        Objects.requireNonNull(reactivatedBy, "Reactivated by must not be null");
        if (this.status != ProgramStatus.INACTIVE) {
            throw new IllegalStateException("Program is already active");
        }

        Instant now = Instant.now();
        this.status = ProgramStatus.ACTIVE;
        this.updatedAt = now;
        this.updatedBy = reactivatedBy;

        domainEvents.add(new ProgramReactivated(id.value(), tenantId, reactivatedBy, now));
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public ProgramId getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public ProgramStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
        }
    }
}
