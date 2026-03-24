package com.klasio.program.domain.model;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.program.domain.event.ProgramCreated;
import com.klasio.program.domain.event.ProgramDeactivated;
import com.klasio.program.domain.event.ProgramReactivated;
import com.klasio.program.domain.event.ProgramUpdated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgramTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String NAME = "Kids Program";
    private static final UUID CREATED_BY = UUID.randomUUID();

    @Nested
    @DisplayName("create() factory")
    class CreateFactory {

        @Test
        @DisplayName("should create program with ACTIVE status and all fields set correctly")
        void create_withValidInputs_returnsActiveProgram() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);

            assertNotNull(program.getId());
            assertNotNull(program.getId().value());
            assertEquals(TENANT_ID, program.getTenantId());
            assertEquals(NAME, program.getName());
            assertEquals(ProgramStatus.ACTIVE, program.getStatus());
            assertNotNull(program.getCreatedAt());
            assertEquals(CREATED_BY, program.getCreatedBy());
        }

        @Test
        @DisplayName("should emit exactly one ProgramCreated domain event")
        void create_withValidInputs_emitsProgramCreatedEvent() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);

            List<DomainEvent> events = program.getDomainEvents();
            assertEquals(1, events.size());

            DomainEvent event = events.get(0);
            assertInstanceOf(ProgramCreated.class, event);

            ProgramCreated created = (ProgramCreated) event;
            assertEquals(program.getId().value(), created.programId());
            assertEquals(TENANT_ID, created.tenantId());
            assertEquals(NAME, created.name());
            assertEquals(CREATED_BY, created.createdBy());
            assertNotNull(created.occurredAt());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should reject blank name")
        void create_withBlankName_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> Program.create(TENANT_ID, "  ", CREATED_BY));
        }

        @Test
        @DisplayName("should reject null name")
        void create_withNullName_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> Program.create(TENANT_ID, null, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null tenantId")
        void create_withNullTenantId_throwsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> Program.create(null, NAME, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null createdBy")
        void create_withNullCreatedBy_throwsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> Program.create(TENANT_ID, NAME, null));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        private static final UUID UPDATED_BY = UUID.randomUUID();
        private static final String NEW_NAME = "Youth & Adults";

        @Test
        @DisplayName("should update name, updatedAt, and updatedBy")
        void update_withValidInputs_updatesFields() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);
            program.clearDomainEvents();

            program.update(NEW_NAME, UPDATED_BY);

            assertEquals(NEW_NAME, program.getName());
            assertNotNull(program.getUpdatedAt());
            assertEquals(UPDATED_BY, program.getUpdatedBy());
        }

        @Test
        @DisplayName("should emit ProgramUpdated event")
        void update_emitsProgramUpdatedEvent() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);
            program.clearDomainEvents();

            program.update(NEW_NAME, UPDATED_BY);

            List<DomainEvent> events = program.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProgramUpdated.class, events.get(0));

            ProgramUpdated event = (ProgramUpdated) events.get(0);
            assertEquals(program.getId().value(), event.programId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(NEW_NAME, event.name());
            assertEquals(UPDATED_BY, event.updatedBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should reject blank name")
        void update_withBlankName_throwsIllegalArgument() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);

            assertThrows(IllegalArgumentException.class,
                    () -> program.update("  ", UPDATED_BY));
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        private static final UUID DEACTIVATED_BY = UUID.randomUUID();

        @Test
        @DisplayName("should set status to INACTIVE when program is ACTIVE")
        void deactivate_whenActive_setsInactive() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);
            program.clearDomainEvents();

            program.deactivate(DEACTIVATED_BY);

            assertEquals(ProgramStatus.INACTIVE, program.getStatus());
        }

        @Test
        @DisplayName("should emit ProgramDeactivated event")
        void deactivate_whenActive_emitsEvent() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);
            program.clearDomainEvents();

            program.deactivate(DEACTIVATED_BY);

            List<DomainEvent> events = program.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProgramDeactivated.class, events.get(0));

            ProgramDeactivated event = (ProgramDeactivated) events.get(0);
            assertEquals(program.getId().value(), event.programId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(DEACTIVATED_BY, event.deactivatedBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should set updatedAt and updatedBy")
        void deactivate_whenActive_setsUpdatedFields() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);
            program.clearDomainEvents();

            program.deactivate(DEACTIVATED_BY);

            assertNotNull(program.getUpdatedAt());
            assertEquals(DEACTIVATED_BY, program.getUpdatedBy());
        }

        @Test
        @DisplayName("should throw IllegalStateException when already inactive")
        void deactivate_whenInactive_throwsIllegalStateException() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);
            program.deactivate(DEACTIVATED_BY);

            assertThrows(IllegalStateException.class,
                    () -> program.deactivate(DEACTIVATED_BY));
        }
    }

    @Nested
    @DisplayName("reactivate()")
    class Reactivate {

        private static final UUID DEACTIVATED_BY = UUID.randomUUID();
        private static final UUID REACTIVATED_BY = UUID.randomUUID();

        @Test
        @DisplayName("should set status to ACTIVE when program is INACTIVE")
        void reactivate_whenInactive_setsActive() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);
            program.deactivate(DEACTIVATED_BY);
            program.clearDomainEvents();

            program.reactivate(REACTIVATED_BY);

            assertEquals(ProgramStatus.ACTIVE, program.getStatus());
        }

        @Test
        @DisplayName("should emit ProgramReactivated event")
        void reactivate_whenInactive_emitsEvent() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);
            program.deactivate(DEACTIVATED_BY);
            program.clearDomainEvents();

            program.reactivate(REACTIVATED_BY);

            List<DomainEvent> events = program.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProgramReactivated.class, events.get(0));

            ProgramReactivated event = (ProgramReactivated) events.get(0);
            assertEquals(program.getId().value(), event.programId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(REACTIVATED_BY, event.reactivatedBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should set updatedAt and updatedBy")
        void reactivate_whenInactive_setsUpdatedFields() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);
            program.deactivate(DEACTIVATED_BY);
            program.clearDomainEvents();

            program.reactivate(REACTIVATED_BY);

            assertNotNull(program.getUpdatedAt());
            assertEquals(REACTIVATED_BY, program.getUpdatedBy());
        }

        @Test
        @DisplayName("should throw IllegalStateException when already active")
        void reactivate_whenActive_throwsIllegalStateException() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);

            assertThrows(IllegalStateException.class,
                    () -> program.reactivate(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("Domain events")
    class DomainEvents {

        @Test
        @DisplayName("should return unmodifiable list from getDomainEvents")
        void shouldReturnUnmodifiableList() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);

            List<DomainEvent> events = program.getDomainEvents();
            assertThrows(UnsupportedOperationException.class, () -> events.clear());
        }

        @Test
        @DisplayName("should clear all domain events when clearDomainEvents is called")
        void shouldClearDomainEvents() {
            Program program = Program.create(TENANT_ID, NAME, CREATED_BY);

            program.clearDomainEvents();

            assertEquals(0, program.getDomainEvents().size());
        }
    }
}
