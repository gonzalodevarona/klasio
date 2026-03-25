package com.klasio.professor.domain.model;

import com.klasio.professor.domain.event.ProfessorCreated;
import com.klasio.professor.domain.event.ProfessorDeactivated;
import com.klasio.professor.domain.event.ProfessorReactivated;
import com.klasio.professor.domain.event.ProfessorUpdated;
import com.klasio.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessorTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String FIRST_NAME = "Carlos";
    private static final String LAST_NAME = "Martinez";
    private static final String EMAIL = "carlos@example.com";
    private static final UUID CREATED_BY = UUID.randomUUID();
    private static final UUID UPDATED_BY = UUID.randomUUID();

    @Nested
    @DisplayName("create() factory")
    class CreateFactory {

        @Test
        @DisplayName("should create professor with INVITED status and all fields set correctly")
        void create_withValidData_returnsProfessorWithInvitedStatus() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);

            assertNotNull(professor.getId());
            assertNotNull(professor.getId().value());
            assertEquals(TENANT_ID, professor.getTenantId());
            assertEquals(FIRST_NAME, professor.getFirstName());
            assertEquals(LAST_NAME, professor.getLastName());
            assertEquals(EMAIL, professor.getEmail());
            assertEquals(ProfessorStatus.INVITED, professor.getStatus());
            assertNotNull(professor.getInvitationToken());
            assertNotNull(professor.getInvitationExpiresAt());
            assertNotNull(professor.getCreatedAt());
            assertEquals(CREATED_BY, professor.getCreatedBy());

            Instant expectedExpiry = Instant.now().plus(72, ChronoUnit.HOURS);
            long diffSeconds = Math.abs(
                    professor.getInvitationExpiresAt().getEpochSecond() - expectedExpiry.getEpochSecond());
            assertTrue(diffSeconds < 5, "Invitation expiry should be ~72h from now");
        }

        @Test
        @DisplayName("should reject null tenantId")
        void create_withNullTenantId_throwsNPE() {
            assertThrows(NullPointerException.class,
                    () -> Professor.create(null, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null createdBy")
        void create_withNullCreatedBy_throwsNPE() {
            assertThrows(NullPointerException.class,
                    () -> Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, null));
        }

        @Test
        @DisplayName("should reject blank firstName")
        void create_withBlankFirstName_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> Professor.create(TENANT_ID, "  ", LAST_NAME, EMAIL, null, CREATED_BY));
        }

        @Test
        @DisplayName("should reject blank lastName")
        void create_withBlankLastName_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> Professor.create(TENANT_ID, FIRST_NAME, "  ", EMAIL, null, CREATED_BY));
        }

        @Test
        @DisplayName("should reject blank email")
        void create_withBlankEmail_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, "  ", null, CREATED_BY));
        }

        @Test
        @DisplayName("should reject invalid email format")
        void create_withInvalidEmail_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, "not-an-email", null, CREATED_BY));
        }

        @Test
        @DisplayName("should capitalize first letter of firstName and lastName")
        void create_withLowercaseNames_capitalizesFirstLetter() {
            Professor professor = Professor.create(TENANT_ID, "carlos", "martinez", EMAIL, null, CREATED_BY);

            assertEquals("Carlos", professor.getFirstName());
            assertEquals("Martinez", professor.getLastName());
        }

        @Test
        @DisplayName("should store email in lowercase")
        void create_withUpperCaseEmail_storesLowerCase() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, "CARLOS@EXAMPLE.COM", null, CREATED_BY);

            assertEquals("carlos@example.com", professor.getEmail());
        }

        @Test
        @DisplayName("should not lowercase the rest of the name")
        void create_withMixedCaseNames_onlyCapitalizesFirstLetter() {
            Professor professor = Professor.create(TENANT_ID, "mcGregor", "de la Cruz", EMAIL, null, CREATED_BY);

            assertEquals("McGregor", professor.getFirstName());
            assertEquals("De la Cruz", professor.getLastName());
        }

        @Test
        @DisplayName("should emit ProfessorCreated event with correct fields")
        void create_publishesProfessorCreatedEvent() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);

            List<DomainEvent> events = professor.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProfessorCreated.class, events.get(0));

            ProfessorCreated event = (ProfessorCreated) events.get(0);
            assertEquals(professor.getId().value(), event.professorId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(FIRST_NAME, event.firstName());
            assertEquals(LAST_NAME, event.lastName());
            assertEquals(EMAIL, event.email());
            assertEquals(professor.getInvitationToken(), event.invitationToken());
            assertEquals(CREATED_BY, event.createdBy());
            assertNotNull(event.occurredAt());
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("should update fields correctly")
        void update_withValidData_updatesFields() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);
            professor.clearDomainEvents();

            professor.update("Ana", "Lopez", "ana@example.com", "+573001234567", UPDATED_BY);

            assertEquals("Ana", professor.getFirstName());
            assertEquals("Lopez", professor.getLastName());
            assertEquals("ana@example.com", professor.getEmail());
            assertNotNull(professor.getUpdatedAt());
            assertEquals(UPDATED_BY, professor.getUpdatedBy());
        }

        @Test
        @DisplayName("should store email in lowercase on update")
        void update_withUpperCaseEmail_storesLowerCase() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);
            professor.update(FIRST_NAME, LAST_NAME, "ANA@EXAMPLE.COM", null, UPDATED_BY);

            assertEquals("ana@example.com", professor.getEmail());
        }

        @Test
        @DisplayName("should capitalize first letter on update")
        void update_withLowercaseNames_capitalizesFirstLetter() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);
            professor.update("ana", "lopez", "ana@example.com", null, UPDATED_BY);

            assertEquals("Ana", professor.getFirstName());
            assertEquals("Lopez", professor.getLastName());
        }

        @Test
        @DisplayName("should reject blank firstName")
        void update_withBlankFirstName_throwsIllegalArgument() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);

            assertThrows(IllegalArgumentException.class,
                    () -> professor.update("  ", LAST_NAME, EMAIL, null, UPDATED_BY));
        }

        @Test
        @DisplayName("should reject blank email")
        void update_withBlankEmail_throwsIllegalArgument() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);

            assertThrows(IllegalArgumentException.class,
                    () -> professor.update(FIRST_NAME, LAST_NAME, "  ", null, UPDATED_BY));
        }

        @Test
        @DisplayName("should emit ProfessorUpdated event")
        void update_publishesProfessorUpdatedEvent() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);
            professor.clearDomainEvents();

            professor.update("Ana", "Lopez", "ana@example.com", "+573001234567", UPDATED_BY);

            List<DomainEvent> events = professor.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProfessorUpdated.class, events.get(0));

            ProfessorUpdated event = (ProfessorUpdated) events.get(0);
            assertEquals(professor.getId().value(), event.professorId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals("Ana", event.firstName());
            assertEquals("Lopez", event.lastName());
            assertEquals("ana@example.com", event.email());
            assertEquals(UPDATED_BY, event.updatedBy());
            assertNotNull(event.occurredAt());
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        private static final UUID DEACTIVATED_BY = UUID.randomUUID();

        @Test
        @DisplayName("should deactivate from ACTIVE status")
        void deactivate_fromActive_setsStatusToDeactivated() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);
            // Activate first by reconstituting as ACTIVE
            Professor active = Professor.reconstitute(
                    professor.getId(), TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null,
                    ProfessorStatus.ACTIVE, null, null,
                    Instant.now(), CREATED_BY, null, null);
            active.deactivate(DEACTIVATED_BY);

            assertEquals(ProfessorStatus.DEACTIVATED, active.getStatus());
        }

        @Test
        @DisplayName("should deactivate from INVITED status")
        void deactivate_fromInvited_setsStatusToDeactivated() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);
            professor.clearDomainEvents();

            professor.deactivate(DEACTIVATED_BY);

            assertEquals(ProfessorStatus.DEACTIVATED, professor.getStatus());
        }

        @Test
        @DisplayName("should throw when already deactivated")
        void deactivate_fromDeactivated_throwsIllegalState() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);
            professor.deactivate(DEACTIVATED_BY);

            assertThrows(IllegalStateException.class,
                    () -> professor.deactivate(DEACTIVATED_BY));
        }

        @Test
        @DisplayName("should emit ProfessorDeactivated event")
        void deactivate_publishesProfessorDeactivatedEvent() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);
            professor.clearDomainEvents();

            professor.deactivate(DEACTIVATED_BY);

            List<DomainEvent> events = professor.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProfessorDeactivated.class, events.get(0));

            ProfessorDeactivated event = (ProfessorDeactivated) events.get(0);
            assertEquals(professor.getId().value(), event.professorId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(DEACTIVATED_BY, event.deactivatedBy());
            assertNotNull(event.occurredAt());
        }
    }

    @Nested
    @DisplayName("reactivate()")
    class Reactivate {

        private static final UUID DEACTIVATED_BY = UUID.randomUUID();
        private static final UUID REACTIVATED_BY = UUID.randomUUID();

        @Test
        @DisplayName("should reactivate from DEACTIVATED status to ACTIVE")
        void reactivate_fromDeactivated_setsStatusToActive() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);
            professor.deactivate(DEACTIVATED_BY);
            professor.clearDomainEvents();

            professor.reactivate(REACTIVATED_BY);

            assertEquals(ProfessorStatus.ACTIVE, professor.getStatus());
        }

        @Test
        @DisplayName("should throw when already active")
        void reactivate_fromActive_throwsIllegalState() {
            Professor active = Professor.reconstitute(
                    ProfessorId.generate(), TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null,
                    ProfessorStatus.ACTIVE, null, null,
                    Instant.now(), CREATED_BY, null, null);

            assertThrows(IllegalStateException.class,
                    () -> active.reactivate(REACTIVATED_BY));
        }

        @Test
        @DisplayName("should throw when status is INVITED")
        void reactivate_fromInvited_throwsIllegalState() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);

            assertThrows(IllegalStateException.class,
                    () -> professor.reactivate(REACTIVATED_BY));
        }

        @Test
        @DisplayName("should emit ProfessorReactivated event")
        void reactivate_publishesProfessorReactivatedEvent() {
            Professor professor = Professor.create(TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null, CREATED_BY);
            professor.deactivate(DEACTIVATED_BY);
            professor.clearDomainEvents();

            professor.reactivate(REACTIVATED_BY);

            List<DomainEvent> events = professor.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProfessorReactivated.class, events.get(0));

            ProfessorReactivated event = (ProfessorReactivated) events.get(0);
            assertEquals(professor.getId().value(), event.professorId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(REACTIVATED_BY, event.reactivatedBy());
            assertNotNull(event.occurredAt());
        }
    }

    @Nested
    @DisplayName("reconstitute()")
    class Reconstitute {

        @Test
        @DisplayName("should create professor without events")
        void reconstitute_createsWithoutEvents() {
            Professor professor = Professor.reconstitute(
                    ProfessorId.generate(), TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL, null,
                    ProfessorStatus.ACTIVE, null, null,
                    Instant.now(), CREATED_BY, null, null);

            assertEquals(0, professor.getDomainEvents().size());
            assertEquals(ProfessorStatus.ACTIVE, professor.getStatus());
            assertEquals(FIRST_NAME, professor.getFirstName());
        }
    }
}
