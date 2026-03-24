package com.klasio.program.domain.model;

import com.klasio.program.domain.event.ProgramPlanCreated;
import com.klasio.program.domain.event.ProgramPlanDeactivated;
import com.klasio.program.domain.event.ProgramPlanReactivated;
import com.klasio.program.domain.event.ProgramPlanUpdated;
import com.klasio.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ProgramPlan {

    private final ProgramPlanId id;
    private final UUID programId;
    private final UUID tenantId;
    private String name;
    private final ProgramModality modality;
    private BigDecimal cost;
    private Integer hours;
    private List<ScheduleEntry> scheduleEntries;
    private UUID managerId;
    private ProgramPlanStatus status;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private ProgramPlan(ProgramPlanId id,
                        UUID programId,
                        UUID tenantId,
                        String name,
                        ProgramModality modality,
                        BigDecimal cost,
                        Integer hours,
                        List<ScheduleEntry> scheduleEntries,
                        UUID managerId,
                        ProgramPlanStatus status,
                        Instant createdAt,
                        UUID createdBy,
                        Instant updatedAt,
                        UUID updatedBy) {
        this.id = id;
        this.programId = programId;
        this.tenantId = tenantId;
        this.name = name;
        this.modality = modality;
        this.cost = cost;
        this.hours = hours;
        this.scheduleEntries = new ArrayList<>(scheduleEntries);
        this.managerId = managerId;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static ProgramPlan create(UUID programId,
                                     UUID tenantId,
                                     String name,
                                     ProgramModality modality,
                                     BigDecimal cost,
                                     Integer hours,
                                     List<ScheduleEntry> scheduleEntries,
                                     UUID managerId,
                                     UUID createdBy) {
        Objects.requireNonNull(programId, "Program id must not be null");
        Objects.requireNonNull(tenantId, "Tenant id must not be null");
        Objects.requireNonNull(modality, "Modality must not be null");
        Objects.requireNonNull(managerId, "Manager id must not be null");
        Objects.requireNonNull(createdBy, "Created by must not be null");
        validateNotBlank(name, "Name");
        validatePositiveCost(cost);
        validateModalityFields(modality, hours, scheduleEntries);

        Instant now = Instant.now();
        ProgramPlanId id = ProgramPlanId.generate();

        ProgramPlan plan = new ProgramPlan(
                id,
                programId,
                tenantId,
                name,
                modality,
                cost,
                hours,
                scheduleEntries != null ? scheduleEntries : Collections.emptyList(),
                managerId,
                ProgramPlanStatus.ACTIVE,
                now,
                createdBy,
                null,
                null
        );

        plan.domainEvents.add(new ProgramPlanCreated(
                id.value(), programId, tenantId, name, modality.name(), cost, managerId, createdBy, now));

        return plan;
    }

    public static ProgramPlan reconstitute(ProgramPlanId id,
                                           UUID programId,
                                           UUID tenantId,
                                           String name,
                                           ProgramModality modality,
                                           BigDecimal cost,
                                           Integer hours,
                                           List<ScheduleEntry> scheduleEntries,
                                           UUID managerId,
                                           ProgramPlanStatus status,
                                           Instant createdAt,
                                           UUID createdBy,
                                           Instant updatedAt,
                                           UUID updatedBy) {
        return new ProgramPlan(id, programId, tenantId, name, modality, cost, hours,
                scheduleEntries, managerId, status, createdAt, createdBy, updatedAt, updatedBy);
    }

    public void update(String name,
                       BigDecimal cost,
                       Integer hours,
                       List<ScheduleEntry> scheduleEntries,
                       UUID managerId,
                       UUID updatedBy) {
        Objects.requireNonNull(managerId, "Manager id must not be null");
        Objects.requireNonNull(updatedBy, "Updated by must not be null");
        validateNotBlank(name, "Name");
        validatePositiveCost(cost);
        validateModalityFields(this.modality, hours, scheduleEntries);

        this.name = name;
        this.cost = cost;
        this.hours = hours;
        this.scheduleEntries = new ArrayList<>(scheduleEntries != null ? scheduleEntries : Collections.emptyList());
        this.managerId = managerId;
        this.updatedAt = Instant.now();
        this.updatedBy = updatedBy;

        domainEvents.add(new ProgramPlanUpdated(
                id.value(), programId, tenantId, name, cost, managerId, updatedBy, this.updatedAt));
    }

    public void deactivate(UUID deactivatedBy) {
        Objects.requireNonNull(deactivatedBy, "Deactivated by must not be null");
        if (this.status != ProgramPlanStatus.ACTIVE) {
            throw new IllegalStateException("Plan is already inactive");
        }

        Instant now = Instant.now();
        this.status = ProgramPlanStatus.INACTIVE;
        this.updatedAt = now;
        this.updatedBy = deactivatedBy;

        domainEvents.add(new ProgramPlanDeactivated(id.value(), programId, tenantId, deactivatedBy, now));
    }

    public void reactivate(UUID reactivatedBy) {
        Objects.requireNonNull(reactivatedBy, "Reactivated by must not be null");
        if (this.status != ProgramPlanStatus.INACTIVE) {
            throw new IllegalStateException("Plan is already active");
        }

        Instant now = Instant.now();
        this.status = ProgramPlanStatus.ACTIVE;
        this.updatedAt = now;
        this.updatedBy = reactivatedBy;

        domainEvents.add(new ProgramPlanReactivated(id.value(), programId, tenantId, reactivatedBy, now));
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public ProgramPlanId getId() { return id; }
    public UUID getProgramId() { return programId; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public ProgramModality getModality() { return modality; }
    public BigDecimal getCost() { return cost; }
    public Integer getHours() { return hours; }
    public List<ScheduleEntry> getScheduleEntries() { return Collections.unmodifiableList(scheduleEntries); }
    public UUID getManagerId() { return managerId; }
    public ProgramPlanStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
        }
    }

    private static void validatePositiveCost(BigDecimal cost) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cost must be positive");
        }
    }

    private static void validateModalityFields(ProgramModality modality, Integer hours, List<ScheduleEntry> scheduleEntries) {
        if (modality == ProgramModality.HOURS_BASED) {
            if (hours == null || hours <= 0) {
                throw new IllegalArgumentException("Hours must be positive for HOURS_BASED plans");
            }
        } else if (modality == ProgramModality.CLASSES_PER_WEEK) {
            if (scheduleEntries == null || scheduleEntries.isEmpty()) {
                throw new IllegalArgumentException("Schedule entries are required for CLASSES_PER_WEEK plans");
            }
        }
    }
}
