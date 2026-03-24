package com.klasio.program.domain.model;

import com.klasio.program.domain.event.ProgramPlanCreated;
import com.klasio.program.domain.event.ProgramPlanDeactivated;
import com.klasio.program.domain.event.ProgramPlanReactivated;
import com.klasio.program.domain.event.ProgramPlanUpdated;
import com.klasio.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgramPlanTest {

    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String NAME = "4 Hours";
    private static final BigDecimal COST = new BigDecimal("90000.00");
    private static final Integer HOURS = 4;
    private static final UUID MANAGER_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();

    private static final ScheduleEntry MONDAY_EVENING = new ScheduleEntry(
            DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(20, 0));
    private static final ScheduleEntry WEDNESDAY_EVENING = new ScheduleEntry(
            DayOfWeek.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(20, 0));

    @Nested
    @DisplayName("create() factory — HOURS_BASED")
    class CreateHoursBased {

        @Test
        @DisplayName("should create plan with ACTIVE status, stored modality, and hours set")
        void create_hoursBased_returnsActivePlan() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);

            assertNotNull(plan.getId());
            assertNotNull(plan.getId().value());
            assertEquals(PROGRAM_ID, plan.getProgramId());
            assertEquals(TENANT_ID, plan.getTenantId());
            assertEquals(NAME, plan.getName());
            assertEquals(ProgramModality.HOURS_BASED, plan.getModality());
            assertEquals(COST, plan.getCost());
            assertEquals(HOURS, plan.getHours());
            assertTrue(plan.getScheduleEntries().isEmpty());
            assertEquals(MANAGER_ID, plan.getManagerId());
            assertEquals(ProgramPlanStatus.ACTIVE, plan.getStatus());
            assertNotNull(plan.getCreatedAt());
            assertEquals(CREATED_BY, plan.getCreatedBy());
        }

        @Test
        @DisplayName("should emit ProgramPlanCreated event with modality and managerId")
        void create_hoursBased_emitsEvent() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);

            List<DomainEvent> events = plan.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProgramPlanCreated.class, events.get(0));

            ProgramPlanCreated event = (ProgramPlanCreated) events.get(0);
            assertEquals(plan.getId().value(), event.planId());
            assertEquals(PROGRAM_ID, event.programId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(NAME, event.name());
            assertEquals("HOURS_BASED", event.modality());
            assertEquals(COST, event.cost());
            assertEquals(MANAGER_ID, event.managerId());
            assertEquals(CREATED_BY, event.createdBy());
            assertNotNull(event.occurredAt());
        }
    }

    @Nested
    @DisplayName("create() factory — CLASSES_PER_WEEK")
    class CreateClassesPerWeek {

        @Test
        @DisplayName("should create plan with schedule entries, null hours, and stored modality")
        void create_classesPerWeek_returnsActivePlan() {
            List<ScheduleEntry> entries = List.of(MONDAY_EVENING, WEDNESDAY_EVENING);

            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, "Mon/Wed Evening", ProgramModality.CLASSES_PER_WEEK, COST, null,
                    entries, MANAGER_ID, CREATED_BY);

            assertNull(plan.getHours());
            assertEquals(2, plan.getScheduleEntries().size());
            assertEquals(ProgramModality.CLASSES_PER_WEEK, plan.getModality());
            assertEquals(ProgramPlanStatus.ACTIVE, plan.getStatus());
        }

        @Test
        @DisplayName("should emit ProgramPlanCreated event")
        void create_classesPerWeek_emitsEvent() {
            List<ScheduleEntry> entries = List.of(MONDAY_EVENING, WEDNESDAY_EVENING);

            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, "Mon/Wed Evening", ProgramModality.CLASSES_PER_WEEK, COST, null,
                    entries, MANAGER_ID, CREATED_BY);

            assertEquals(1, plan.getDomainEvents().size());
            assertInstanceOf(ProgramPlanCreated.class, plan.getDomainEvents().get(0));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should reject blank name")
        void create_withBlankName_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, "  ", ProgramModality.HOURS_BASED, COST, HOURS,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null name")
        void create_withNullName_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, null, ProgramModality.HOURS_BASED, COST, HOURS,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null cost")
        void create_withNullCost_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, null, HOURS,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject zero cost")
        void create_withZeroCost_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, BigDecimal.ZERO, HOURS,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject negative cost")
        void create_withNegativeCost_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, new BigDecimal("-100"), HOURS,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null programId")
        void create_withNullProgramId_throwsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> ProgramPlan.create(null, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null tenantId")
        void create_withNullTenantId_throwsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, null, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null createdBy")
        void create_withNullCreatedBy_throwsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                            Collections.emptyList(), MANAGER_ID, null));
        }

        @Test
        @DisplayName("should reject null modality")
        void create_withNullModality_throwsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, NAME, null, COST, HOURS,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null managerId on create")
        void create_withNullManagerId_throwsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                            Collections.emptyList(), null, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null hours for HOURS_BASED")
        void create_hoursBased_withNullHours_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, null,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject zero hours for HOURS_BASED")
        void create_hoursBased_withZeroHours_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, 0,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject negative hours for HOURS_BASED")
        void create_hoursBased_withNegativeHours_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, -2,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject empty schedule entries for CLASSES_PER_WEEK")
        void create_classesPerWeek_withEmptySchedule_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, NAME, ProgramModality.CLASSES_PER_WEEK, COST, null,
                            Collections.emptyList(), MANAGER_ID, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null schedule entries for CLASSES_PER_WEEK")
        void create_classesPerWeek_withNullSchedule_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramPlan.create(PROGRAM_ID, TENANT_ID, NAME, ProgramModality.CLASSES_PER_WEEK, COST, null,
                            null, MANAGER_ID, CREATED_BY));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        private static final UUID UPDATED_BY = UUID.randomUUID();
        private static final UUID NEW_MANAGER_ID = UUID.randomUUID();
        private static final String NEW_NAME = "8 Hours";
        private static final BigDecimal NEW_COST = new BigDecimal("160000.00");
        private static final Integer NEW_HOURS = 8;

        @Test
        @DisplayName("should update mutable fields for HOURS_BASED plan using stored modality")
        void update_hoursBased_updatesFields() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);
            plan.clearDomainEvents();

            plan.update(NEW_NAME, NEW_COST, NEW_HOURS,
                    Collections.emptyList(), NEW_MANAGER_ID, UPDATED_BY);

            assertEquals(NEW_NAME, plan.getName());
            assertEquals(NEW_COST, plan.getCost());
            assertEquals(NEW_HOURS, plan.getHours());
            assertEquals(NEW_MANAGER_ID, plan.getManagerId());
            assertEquals(ProgramModality.HOURS_BASED, plan.getModality());
            assertNotNull(plan.getUpdatedAt());
            assertEquals(UPDATED_BY, plan.getUpdatedBy());
        }

        @Test
        @DisplayName("should update schedule entries for CLASSES_PER_WEEK plan")
        void update_classesPerWeek_updatesSchedule() {
            List<ScheduleEntry> originalEntries = List.of(MONDAY_EVENING);
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, "Mon Evening", ProgramModality.CLASSES_PER_WEEK, COST, null,
                    originalEntries, MANAGER_ID, CREATED_BY);
            plan.clearDomainEvents();

            List<ScheduleEntry> newEntries = List.of(MONDAY_EVENING, WEDNESDAY_EVENING);
            plan.update("Mon/Wed Evening", NEW_COST, null,
                    newEntries, NEW_MANAGER_ID, UPDATED_BY);

            assertEquals(2, plan.getScheduleEntries().size());
            assertEquals("Mon/Wed Evening", plan.getName());
            assertEquals(NEW_MANAGER_ID, plan.getManagerId());
        }

        @Test
        @DisplayName("should emit ProgramPlanUpdated event with managerId")
        void update_emitsEvent() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);
            plan.clearDomainEvents();

            plan.update(NEW_NAME, NEW_COST, NEW_HOURS,
                    Collections.emptyList(), NEW_MANAGER_ID, UPDATED_BY);

            List<DomainEvent> events = plan.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProgramPlanUpdated.class, events.get(0));

            ProgramPlanUpdated event = (ProgramPlanUpdated) events.get(0);
            assertEquals(plan.getId().value(), event.planId());
            assertEquals(PROGRAM_ID, event.programId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(NEW_NAME, event.name());
            assertEquals(NEW_COST, event.cost());
            assertEquals(NEW_MANAGER_ID, event.managerId());
            assertEquals(UPDATED_BY, event.updatedBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should reject blank name")
        void update_withBlankName_throwsIllegalArgument() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);

            assertThrows(IllegalArgumentException.class,
                    () -> plan.update("  ", NEW_COST, NEW_HOURS,
                            Collections.emptyList(), NEW_MANAGER_ID, UPDATED_BY));
        }

        @Test
        @DisplayName("should reject negative cost")
        void update_withNegativeCost_throwsIllegalArgument() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);

            assertThrows(IllegalArgumentException.class,
                    () -> plan.update(NEW_NAME, new BigDecimal("-100"), NEW_HOURS,
                            Collections.emptyList(), NEW_MANAGER_ID, UPDATED_BY));
        }

        @Test
        @DisplayName("should reject null managerId on update")
        void update_withNullManagerId_throwsNullPointerException() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);

            assertThrows(NullPointerException.class,
                    () -> plan.update(NEW_NAME, NEW_COST, NEW_HOURS,
                            Collections.emptyList(), null, UPDATED_BY));
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        private static final UUID DEACTIVATED_BY = UUID.randomUUID();

        @Test
        @DisplayName("should set status to INACTIVE when plan is ACTIVE")
        void deactivate_whenActive_setsInactive() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);
            plan.clearDomainEvents();

            plan.deactivate(DEACTIVATED_BY);

            assertEquals(ProgramPlanStatus.INACTIVE, plan.getStatus());
        }

        @Test
        @DisplayName("should emit ProgramPlanDeactivated event")
        void deactivate_whenActive_emitsEvent() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);
            plan.clearDomainEvents();

            plan.deactivate(DEACTIVATED_BY);

            List<DomainEvent> events = plan.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProgramPlanDeactivated.class, events.get(0));

            ProgramPlanDeactivated event = (ProgramPlanDeactivated) events.get(0);
            assertEquals(plan.getId().value(), event.planId());
            assertEquals(PROGRAM_ID, event.programId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(DEACTIVATED_BY, event.deactivatedBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should set updatedAt and updatedBy")
        void deactivate_whenActive_setsUpdatedFields() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);
            plan.clearDomainEvents();

            plan.deactivate(DEACTIVATED_BY);

            assertNotNull(plan.getUpdatedAt());
            assertEquals(DEACTIVATED_BY, plan.getUpdatedBy());
        }

        @Test
        @DisplayName("should throw when already inactive")
        void deactivate_whenInactive_throwsIllegalState() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);
            plan.deactivate(DEACTIVATED_BY);

            assertThrows(IllegalStateException.class,
                    () -> plan.deactivate(DEACTIVATED_BY));
        }
    }

    @Nested
    @DisplayName("reactivate()")
    class Reactivate {

        private static final UUID DEACTIVATED_BY = UUID.randomUUID();
        private static final UUID REACTIVATED_BY = UUID.randomUUID();

        @Test
        @DisplayName("should set status to ACTIVE when plan is INACTIVE")
        void reactivate_whenInactive_setsActive() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);
            plan.deactivate(DEACTIVATED_BY);
            plan.clearDomainEvents();

            plan.reactivate(REACTIVATED_BY);

            assertEquals(ProgramPlanStatus.ACTIVE, plan.getStatus());
        }

        @Test
        @DisplayName("should emit ProgramPlanReactivated event")
        void reactivate_whenInactive_emitsEvent() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);
            plan.deactivate(DEACTIVATED_BY);
            plan.clearDomainEvents();

            plan.reactivate(REACTIVATED_BY);

            List<DomainEvent> events = plan.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProgramPlanReactivated.class, events.get(0));

            ProgramPlanReactivated event = (ProgramPlanReactivated) events.get(0);
            assertEquals(plan.getId().value(), event.planId());
            assertEquals(PROGRAM_ID, event.programId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(REACTIVATED_BY, event.reactivatedBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should set updatedAt and updatedBy")
        void reactivate_whenInactive_setsUpdatedFields() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);
            plan.deactivate(DEACTIVATED_BY);
            plan.clearDomainEvents();

            plan.reactivate(REACTIVATED_BY);

            assertNotNull(plan.getUpdatedAt());
            assertEquals(REACTIVATED_BY, plan.getUpdatedBy());
        }

        @Test
        @DisplayName("should throw when already active")
        void reactivate_whenActive_throwsIllegalState() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);

            assertThrows(IllegalStateException.class,
                    () -> plan.reactivate(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("Domain events")
    class DomainEventsTest {

        @Test
        @DisplayName("should return unmodifiable list from getDomainEvents")
        void shouldReturnUnmodifiableList() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);

            List<DomainEvent> events = plan.getDomainEvents();
            assertThrows(UnsupportedOperationException.class, () -> events.clear());
        }

        @Test
        @DisplayName("should clear all domain events")
        void shouldClearDomainEvents() {
            ProgramPlan plan = ProgramPlan.create(
                    PROGRAM_ID, TENANT_ID, NAME, ProgramModality.HOURS_BASED, COST, HOURS,
                    Collections.emptyList(), MANAGER_ID, CREATED_BY);

            plan.clearDomainEvents();

            assertEquals(0, plan.getDomainEvents().size());
        }
    }
}
