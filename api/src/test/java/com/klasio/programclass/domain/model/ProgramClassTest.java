package com.klasio.programclass.domain.model;

import com.klasio.programclass.domain.event.ClassCreated;
import com.klasio.programclass.domain.event.ClassDeactivated;
import com.klasio.programclass.domain.event.ClassReactivated;
import com.klasio.programclass.domain.event.ClassUpdated;
import com.klasio.programclass.domain.event.ProfessorAssignedToClass;
import com.klasio.programclass.domain.event.ProfessorRemovedFromClass;
import com.klasio.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgramClassTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final String NAME = "Kids Beginner Monday";
    private static final UUID CREATED_BY = UUID.randomUUID();
    private static final UUID UPDATED_BY = UUID.randomUUID();
    private static final int MAX_STUDENTS = 20;

    private static final ClassScheduleEntry MONDAY_SCHEDULE = new ClassScheduleEntry(
            DayOfWeek.MONDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0), null);
    private static final ClassScheduleEntry WEDNESDAY_SCHEDULE = new ClassScheduleEntry(
            DayOfWeek.WEDNESDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0), null);

    private static ClassScheduleEntry oneTimeSchedule() {
        return new ClassScheduleEntry(
                null, LocalDate.now().plusDays(7), LocalTime.of(10, 0), LocalTime.of(12, 0), null);
    }

    // ---- T018: create() for recurring class ----

    @Nested
    @DisplayName("create() — recurring class")
    class CreateRecurring {

        @Test
        @DisplayName("should create recurring class with all fields set and status ACTIVE")
        void create_recurringWithValidInputs_returnsActiveClass() {
            List<ClassScheduleEntry> schedules = List.of(MONDAY_SCHEDULE, WEDNESDAY_SCHEDULE);

            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    schedules, null, MAX_STUDENTS, CREATED_BY);

            assertNotNull(pc.getId());
            assertNotNull(pc.getId().value());
            assertEquals(TENANT_ID, pc.getTenantId());
            assertEquals(PROGRAM_ID, pc.getProgramId());
            assertEquals(NAME, pc.getName());
            assertEquals(ClassLevel.BEGINNER, pc.getLevel());
            assertEquals(ClassType.RECURRING, pc.getType());
            assertEquals(ClassStatus.ACTIVE, pc.getStatus());
            assertNull(pc.getProfessorId());
            assertEquals(MAX_STUDENTS, pc.getMaxStudents());
            assertEquals(2, pc.getScheduleEntries().size());
            assertNotNull(pc.getCreatedAt());
            assertEquals(CREATED_BY, pc.getCreatedBy());
            assertNull(pc.getUpdatedAt());
            assertNull(pc.getUpdatedBy());
        }

        @Test
        @DisplayName("should emit ClassCreated event with correct fields")
        void create_recurringClass_emitsClassCreatedEvent() {
            List<ClassScheduleEntry> schedules = List.of(MONDAY_SCHEDULE);

            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    schedules, null, MAX_STUDENTS, CREATED_BY);

            List<DomainEvent> events = pc.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ClassCreated.class, events.get(0));

            ClassCreated event = (ClassCreated) events.get(0);
            assertEquals(pc.getId().value(), event.classId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(PROGRAM_ID, event.programId());
            assertEquals(NAME, event.name());
            assertEquals("BEGINNER", event.level());
            assertEquals("RECURRING", event.type());
            assertEquals(MAX_STUDENTS, event.maxStudents());
            assertNull(event.professorId());
            assertEquals(CREATED_BY, event.createdBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should validate schedule entries have dayOfWeek and no specificDate")
        void create_recurringClass_schedulesHaveDayOfWeek() {
            List<ClassScheduleEntry> schedules = List.of(MONDAY_SCHEDULE, WEDNESDAY_SCHEDULE);

            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    schedules, null, MAX_STUDENTS, CREATED_BY);

            for (ClassScheduleEntry entry : pc.getScheduleEntries()) {
                assertNotNull(entry.dayOfWeek());
                assertNull(entry.specificDate());
            }
        }

        @Test
        @DisplayName("should create recurring class with professor assigned")
        void create_recurringWithProfessor_setsProfessorId() {
            UUID professorId = UUID.randomUUID();
            List<ClassScheduleEntry> schedules = List.of(MONDAY_SCHEDULE);

            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.INTERMEDIATE, ClassType.RECURRING,
                    schedules, professorId, MAX_STUDENTS, CREATED_BY);

            assertEquals(professorId, pc.getProfessorId());

            ClassCreated event = (ClassCreated) pc.getDomainEvents().get(0);
            assertEquals(professorId, event.professorId());
        }
    }

    // ---- T019: create() for one-time class ----

    @Nested
    @DisplayName("create() — one-time class")
    class CreateOneTime {

        @Test
        @DisplayName("should create one-time class with specificDate and no dayOfWeek")
        void create_oneTimeWithValidInputs_returnsActiveClass() {
            ClassScheduleEntry schedule = oneTimeSchedule();
            List<ClassScheduleEntry> schedules = List.of(schedule);

            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, "Special Workshop", ClassLevel.ADVANCED, ClassType.ONE_TIME,
                    schedules, null, 15, CREATED_BY);

            assertEquals(ClassType.ONE_TIME, pc.getType());
            assertEquals(ClassStatus.ACTIVE, pc.getStatus());
            assertEquals(1, pc.getScheduleEntries().size());

            ClassScheduleEntry entry = pc.getScheduleEntries().get(0);
            assertNotNull(entry.specificDate());
            assertNull(entry.dayOfWeek());
        }

        @Test
        @DisplayName("should reject one-time class with more than one schedule entry")
        void create_oneTimeWithMultipleEntries_throwsIllegalArgument() {
            ClassScheduleEntry entry1 = oneTimeSchedule();
            ClassScheduleEntry entry2 = new ClassScheduleEntry(
                    null, LocalDate.now().plusDays(14), LocalTime.of(10, 0), LocalTime.of(12, 0), null);

            assertThrows(IllegalArgumentException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, "Workshop", ClassLevel.BEGINNER, ClassType.ONE_TIME,
                            List.of(entry1, entry2), null, 10, CREATED_BY));
        }

        @Test
        @DisplayName("should reject one-time class with past specificDate")
        void create_oneTimeWithPastDate_throwsIllegalArgument() {
            ClassScheduleEntry pastEntry = new ClassScheduleEntry(
                    null, LocalDate.now().minusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0), null);

            assertThrows(IllegalArgumentException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, "Workshop", ClassLevel.BEGINNER, ClassType.ONE_TIME,
                            List.of(pastEntry), null, 10, CREATED_BY));
        }

        @Test
        @DisplayName("should reject one-time class with schedule entry that has dayOfWeek instead of specificDate")
        void create_oneTimeWithDayOfWeek_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, "Workshop", ClassLevel.BEGINNER, ClassType.ONE_TIME,
                            List.of(MONDAY_SCHEDULE), null, 10, CREATED_BY));
        }

        @Test
        @DisplayName("should emit ClassCreated event for one-time class")
        void create_oneTimeClass_emitsClassCreatedEvent() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, "Workshop", ClassLevel.ADVANCED, ClassType.ONE_TIME,
                    List.of(oneTimeSchedule()), null, 15, CREATED_BY);

            List<DomainEvent> events = pc.getDomainEvents();
            assertEquals(1, events.size());

            ClassCreated event = (ClassCreated) events.get(0);
            assertEquals("ONE_TIME", event.type());
            assertEquals("ADVANCED", event.level());
        }
    }

    // ---- T020: create() validation failures ----

    @Nested
    @DisplayName("create() — validation")
    class CreateValidation {

        @Test
        @DisplayName("should reject null tenantId")
        void create_withNullTenantId_throwsNPE() {
            assertThrows(NullPointerException.class,
                    () -> ProgramClass.create(
                            null, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                            List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null programId")
        void create_withNullProgramId_throwsNPE() {
            assertThrows(NullPointerException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, null, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                            List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null createdBy")
        void create_withNullCreatedBy_throwsNPE() {
            assertThrows(NullPointerException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                            List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, null));
        }

        @Test
        @DisplayName("should reject blank name")
        void create_withBlankName_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, "  ", ClassLevel.BEGINNER, ClassType.RECURRING,
                            List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null name")
        void create_withNullName_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, null, ClassLevel.BEGINNER, ClassType.RECURRING,
                            List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY));
        }

        @Test
        @DisplayName("should reject name longer than 100 characters")
        void create_withNameTooLong_throwsIllegalArgument() {
            String longName = "A".repeat(101);
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, longName, ClassLevel.BEGINNER, ClassType.RECURRING,
                            List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null level")
        void create_withNullLevel_throwsNPE() {
            assertThrows(NullPointerException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, NAME, null, ClassType.RECURRING,
                            List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null type")
        void create_withNullType_throwsNPE() {
            assertThrows(NullPointerException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, null,
                            List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY));
        }

        @Test
        @DisplayName("should reject empty schedule entries")
        void create_withEmptyScheduleEntries_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                            List.of(), null, MAX_STUDENTS, CREATED_BY));
        }

        @Test
        @DisplayName("should reject null schedule entries")
        void create_withNullScheduleEntries_throwsNPE() {
            assertThrows(NullPointerException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                            null, null, MAX_STUDENTS, CREATED_BY));
        }

        @Test
        @DisplayName("should reject maxStudents of zero")
        void create_withZeroMaxStudents_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                            List.of(MONDAY_SCHEDULE), null, 0, CREATED_BY));
        }

        @Test
        @DisplayName("should reject negative maxStudents")
        void create_withNegativeMaxStudents_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                            List.of(MONDAY_SCHEDULE), null, -5, CREATED_BY));
        }

        @Test
        @DisplayName("should reject recurring class with specificDate schedule entry")
        void create_recurringWithSpecificDate_throwsIllegalArgument() {
            ClassScheduleEntry specificDateEntry = oneTimeSchedule();
            assertThrows(IllegalArgumentException.class,
                    () -> ProgramClass.create(
                            TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                            List.of(specificDateEntry), null, MAX_STUDENTS, CREATED_BY));
        }
    }

    // ---- T021: update() ----

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("should update name, level, schedule, and maxStudents")
        void update_withValidData_updatesFields() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);
            pc.clearDomainEvents();

            List<ClassScheduleEntry> newSchedules = List.of(WEDNESDAY_SCHEDULE);
            pc.update("Advanced Wednesday", ClassLevel.ADVANCED, newSchedules, 30, UPDATED_BY);

            assertEquals("Advanced Wednesday", pc.getName());
            assertEquals(ClassLevel.ADVANCED, pc.getLevel());
            assertEquals(1, pc.getScheduleEntries().size());
            assertEquals(DayOfWeek.WEDNESDAY, pc.getScheduleEntries().get(0).dayOfWeek());
            assertEquals(30, pc.getMaxStudents());
            assertNotNull(pc.getUpdatedAt());
            assertEquals(UPDATED_BY, pc.getUpdatedBy());
        }

        @Test
        @DisplayName("should emit ClassUpdated event")
        void update_emitsClassUpdatedEvent() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);
            pc.clearDomainEvents();

            pc.update("Updated Name", ClassLevel.INTERMEDIATE, List.of(MONDAY_SCHEDULE), 25, UPDATED_BY);

            List<DomainEvent> events = pc.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ClassUpdated.class, events.get(0));

            ClassUpdated event = (ClassUpdated) events.get(0);
            assertEquals(pc.getId().value(), event.classId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(PROGRAM_ID, event.programId());
            assertEquals("Updated Name", event.name());
            assertEquals("INTERMEDIATE", event.level());
            assertEquals(25, event.maxStudents());
            assertEquals(UPDATED_BY, event.updatedBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should not change type on update (type is immutable)")
        void update_typeRemainsImmutable() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);
            pc.clearDomainEvents();

            pc.update("New Name", ClassLevel.ADVANCED, List.of(WEDNESDAY_SCHEDULE), 30, UPDATED_BY);

            assertEquals(ClassType.RECURRING, pc.getType());
        }

        @Test
        @DisplayName("should reject blank name on update")
        void update_withBlankName_throwsIllegalArgument() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);

            assertThrows(IllegalArgumentException.class,
                    () -> pc.update("  ", ClassLevel.BEGINNER, List.of(MONDAY_SCHEDULE), MAX_STUDENTS, UPDATED_BY));
        }

        @Test
        @DisplayName("should reject empty schedule entries on update")
        void update_withEmptySchedule_throwsIllegalArgument() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);

            assertThrows(IllegalArgumentException.class,
                    () -> pc.update(NAME, ClassLevel.BEGINNER, List.of(), MAX_STUDENTS, UPDATED_BY));
        }

        @Test
        @DisplayName("should reject invalid maxStudents on update")
        void update_withZeroMaxStudents_throwsIllegalArgument() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);

            assertThrows(IllegalArgumentException.class,
                    () -> pc.update(NAME, ClassLevel.BEGINNER, List.of(MONDAY_SCHEDULE), 0, UPDATED_BY));
        }

        @Test
        @DisplayName("should reject update with schedule entry type inconsistent with class type")
        void update_recurringWithSpecificDate_throwsIllegalArgument() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);

            assertThrows(IllegalArgumentException.class,
                    () -> pc.update(NAME, ClassLevel.BEGINNER, List.of(oneTimeSchedule()), MAX_STUDENTS, UPDATED_BY));
        }
    }

    // ---- T022: deactivate() ----

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        private static final UUID DEACTIVATED_BY = UUID.randomUUID();

        @Test
        @DisplayName("should set status to INACTIVE when class is ACTIVE")
        void deactivate_whenActive_setsInactive() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);
            pc.clearDomainEvents();

            pc.deactivate(DEACTIVATED_BY);

            assertEquals(ClassStatus.INACTIVE, pc.getStatus());
            assertNotNull(pc.getUpdatedAt());
            assertEquals(DEACTIVATED_BY, pc.getUpdatedBy());
        }

        @Test
        @DisplayName("should emit ClassDeactivated event")
        void deactivate_emitsClassDeactivatedEvent() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);
            pc.clearDomainEvents();

            pc.deactivate(DEACTIVATED_BY);

            List<DomainEvent> events = pc.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ClassDeactivated.class, events.get(0));

            ClassDeactivated event = (ClassDeactivated) events.get(0);
            assertEquals(pc.getId().value(), event.classId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(PROGRAM_ID, event.programId());
            assertEquals(DEACTIVATED_BY, event.deactivatedBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should throw when already inactive")
        void deactivate_whenInactive_throwsIllegalState() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);
            pc.deactivate(DEACTIVATED_BY);

            assertThrows(IllegalStateException.class,
                    () -> pc.deactivate(DEACTIVATED_BY));
        }
    }

    // ---- T023: reactivate() ----

    @Nested
    @DisplayName("reactivate()")
    class Reactivate {

        private static final UUID DEACTIVATED_BY = UUID.randomUUID();
        private static final UUID REACTIVATED_BY = UUID.randomUUID();

        @Test
        @DisplayName("should set status to ACTIVE when class is INACTIVE")
        void reactivate_whenInactive_setsActive() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);
            pc.deactivate(DEACTIVATED_BY);
            pc.clearDomainEvents();

            pc.reactivate(REACTIVATED_BY);

            assertEquals(ClassStatus.ACTIVE, pc.getStatus());
            assertNotNull(pc.getUpdatedAt());
            assertEquals(REACTIVATED_BY, pc.getUpdatedBy());
        }

        @Test
        @DisplayName("should emit ClassReactivated event")
        void reactivate_emitsClassReactivatedEvent() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);
            pc.deactivate(DEACTIVATED_BY);
            pc.clearDomainEvents();

            pc.reactivate(REACTIVATED_BY);

            List<DomainEvent> events = pc.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ClassReactivated.class, events.get(0));

            ClassReactivated event = (ClassReactivated) events.get(0);
            assertEquals(pc.getId().value(), event.classId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(PROGRAM_ID, event.programId());
            assertEquals(REACTIVATED_BY, event.reactivatedBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should throw when already active")
        void reactivate_whenActive_throwsIllegalState() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);

            assertThrows(IllegalStateException.class,
                    () -> pc.reactivate(REACTIVATED_BY));
        }
    }

    // ---- T024: assignProfessor() ----

    @Nested
    @DisplayName("assignProfessor()")
    class AssignProfessor {

        private static final UUID PROFESSOR_ID = UUID.randomUUID();
        private static final UUID ASSIGNED_BY = UUID.randomUUID();

        @Test
        @DisplayName("should set professorId when no professor is currently assigned")
        void assignProfessor_whenNoneAssigned_setsProfessorId() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);
            pc.clearDomainEvents();

            pc.assignProfessor(PROFESSOR_ID, ASSIGNED_BY);

            assertEquals(PROFESSOR_ID, pc.getProfessorId());
            assertNotNull(pc.getUpdatedAt());
            assertEquals(ASSIGNED_BY, pc.getUpdatedBy());
        }

        @Test
        @DisplayName("should emit ProfessorAssignedToClass event")
        void assignProfessor_emitsProfessorAssignedEvent() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);
            pc.clearDomainEvents();

            pc.assignProfessor(PROFESSOR_ID, ASSIGNED_BY);

            List<DomainEvent> events = pc.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProfessorAssignedToClass.class, events.get(0));

            ProfessorAssignedToClass event = (ProfessorAssignedToClass) events.get(0);
            assertEquals(pc.getId().value(), event.classId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(PROGRAM_ID, event.programId());
            assertEquals(PROFESSOR_ID, event.professorId());
            assertEquals(ASSIGNED_BY, event.assignedBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should replace previous professor on reassignment")
        void assignProfessor_whenAlreadyAssigned_replacesPrevious() {
            UUID firstProfessor = UUID.randomUUID();
            UUID secondProfessor = UUID.randomUUID();

            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), firstProfessor, MAX_STUDENTS, CREATED_BY);
            pc.clearDomainEvents();

            pc.assignProfessor(secondProfessor, ASSIGNED_BY);

            assertEquals(secondProfessor, pc.getProfessorId());

            // Should emit both removal and assignment events
            List<DomainEvent> events = pc.getDomainEvents();
            assertEquals(2, events.size());
            assertInstanceOf(ProfessorRemovedFromClass.class, events.get(0));
            assertInstanceOf(ProfessorAssignedToClass.class, events.get(1));

            ProfessorRemovedFromClass removeEvent = (ProfessorRemovedFromClass) events.get(0);
            assertEquals(firstProfessor, removeEvent.previousProfessorId());

            ProfessorAssignedToClass assignEvent = (ProfessorAssignedToClass) events.get(1);
            assertEquals(secondProfessor, assignEvent.professorId());
        }

        @Test
        @DisplayName("should reject null professorId")
        void assignProfessor_withNullProfessorId_throwsNPE() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);

            assertThrows(NullPointerException.class,
                    () -> pc.assignProfessor(null, ASSIGNED_BY));
        }
    }

    // ---- T025: removeProfessor() ----

    @Nested
    @DisplayName("removeProfessor()")
    class RemoveProfessor {

        private static final UUID PROFESSOR_ID = UUID.randomUUID();
        private static final UUID REMOVED_BY = UUID.randomUUID();

        @Test
        @DisplayName("should clear professorId when professor is assigned")
        void removeProfessor_whenAssigned_clearsProfessorId() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), PROFESSOR_ID, MAX_STUDENTS, CREATED_BY);
            pc.clearDomainEvents();

            pc.removeProfessor(REMOVED_BY);

            assertNull(pc.getProfessorId());
            assertNotNull(pc.getUpdatedAt());
            assertEquals(REMOVED_BY, pc.getUpdatedBy());
        }

        @Test
        @DisplayName("should emit ProfessorRemovedFromClass event")
        void removeProfessor_emitsProfessorRemovedEvent() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), PROFESSOR_ID, MAX_STUDENTS, CREATED_BY);
            pc.clearDomainEvents();

            pc.removeProfessor(REMOVED_BY);

            List<DomainEvent> events = pc.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProfessorRemovedFromClass.class, events.get(0));

            ProfessorRemovedFromClass event = (ProfessorRemovedFromClass) events.get(0);
            assertEquals(pc.getId().value(), event.classId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(PROGRAM_ID, event.programId());
            assertEquals(PROFESSOR_ID, event.previousProfessorId());
            assertEquals(REMOVED_BY, event.removedBy());
            assertNotNull(event.occurredAt());
        }

        @Test
        @DisplayName("should throw when no professor is assigned")
        void removeProfessor_whenNoneAssigned_throwsIllegalState() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);

            assertThrows(IllegalStateException.class,
                    () -> pc.removeProfessor(REMOVED_BY));
        }
    }

    // ---- Domain events lifecycle ----

    @Nested
    @DisplayName("Domain events")
    class DomainEvents {

        @Test
        @DisplayName("should return unmodifiable list from getDomainEvents")
        void getDomainEvents_returnsUnmodifiableList() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);

            List<DomainEvent> events = pc.getDomainEvents();
            assertThrows(UnsupportedOperationException.class, () -> events.clear());
        }

        @Test
        @DisplayName("should clear all domain events when clearDomainEvents is called")
        void clearDomainEvents_removesAllEvents() {
            ProgramClass pc = ProgramClass.create(
                    TENANT_ID, PROGRAM_ID, NAME, ClassLevel.BEGINNER, ClassType.RECURRING,
                    List.of(MONDAY_SCHEDULE), null, MAX_STUDENTS, CREATED_BY);

            pc.clearDomainEvents();
            assertEquals(0, pc.getDomainEvents().size());
        }
    }

    // ---- Reconstitute ----

    @Nested
    @DisplayName("reconstitute()")
    class Reconstitute {

        @Test
        @DisplayName("should create ProgramClass without events")
        void reconstitute_createsWithoutEvents() {
            ProgramClass pc = ProgramClass.reconstitute(
                    ProgramClassId.generate(), TENANT_ID, PROGRAM_ID, NAME,
                    ClassLevel.BEGINNER, ClassType.RECURRING,
                    UUID.randomUUID(), MAX_STUDENTS, ClassStatus.ACTIVE,
                    List.of(MONDAY_SCHEDULE),
                    java.time.Instant.now(), CREATED_BY, null, null);

            assertEquals(0, pc.getDomainEvents().size());
            assertEquals(ClassStatus.ACTIVE, pc.getStatus());
            assertEquals(NAME, pc.getName());
            assertEquals(ClassType.RECURRING, pc.getType());
        }
    }
}
