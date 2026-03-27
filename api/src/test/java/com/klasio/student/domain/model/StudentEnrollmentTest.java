package com.klasio.student.domain.model;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.student.domain.event.StudentEnrolled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentEnrollmentTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final Level LEVEL = Level.BEGINNER;
    private static final UUID CREATED_BY = UUID.randomUUID();

    @Nested
    @DisplayName("create() factory")
    class CreateFactory {

        @Test
        @DisplayName("should create enrollment with ACTIVE status")
        void shouldCreateWithActiveStatus() {
            StudentEnrollment enrollment = StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY);

            assertThat(enrollment.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should generate a non-null id")
        void shouldGenerateId() {
            StudentEnrollment enrollment = StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY);

            assertThat(enrollment.getId()).isNotNull();
            assertThat(enrollment.getId().value()).isNotNull();
        }

        @Test
        @DisplayName("should set enrollmentDate to today")
        void shouldSetEnrollmentDateToToday() {
            StudentEnrollment enrollment = StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY);

            assertThat(enrollment.getEnrollmentDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("should store the specified level")
        void shouldStoreLevel() {
            StudentEnrollment enrollment = StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, Level.ADVANCED, CREATED_BY);

            assertThat(enrollment.getLevel()).isEqualTo(Level.ADVANCED);
        }

        @Test
        @DisplayName("should store all provided fields")
        void shouldStoreAllFields() {
            StudentEnrollment enrollment = StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY);

            assertThat(enrollment.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(enrollment.getStudentId()).isEqualTo(STUDENT_ID);
            assertThat(enrollment.getProgramId()).isEqualTo(PROGRAM_ID);
            assertThat(enrollment.getLevel()).isEqualTo(LEVEL);
            assertThat(enrollment.getCreatedBy()).isEqualTo(CREATED_BY);
            assertThat(enrollment.getCreatedAt()).isNotNull();
            assertThat(enrollment.getUpdatedAt()).isNull();
            assertThat(enrollment.getUpdatedBy()).isNull();
        }

        @Test
        @DisplayName("should publish StudentEnrolled domain event")
        void shouldPublishStudentEnrolledEvent() {
            StudentEnrollment enrollment = StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY);

            List<DomainEvent> events = enrollment.getDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(StudentEnrolled.class);

            StudentEnrolled enrolled = (StudentEnrolled) events.get(0);
            assertThat(enrolled.enrollmentId()).isEqualTo(enrollment.getId().value());
            assertThat(enrolled.tenantId()).isEqualTo(TENANT_ID);
            assertThat(enrolled.studentId()).isEqualTo(STUDENT_ID);
            assertThat(enrolled.programId()).isEqualTo(PROGRAM_ID);
            assertThat(enrolled.level()).isEqualTo(LEVEL.name());
            assertThat(enrolled.createdBy()).isEqualTo(CREATED_BY);
            assertThat(enrolled.occurredAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should reject null tenantId")
        void shouldRejectNullTenantId() {
            assertThatThrownBy(() -> StudentEnrollment.create(null, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Tenant id must not be null");
        }

        @Test
        @DisplayName("should reject null studentId")
        void shouldRejectNullStudentId() {
            assertThatThrownBy(() -> StudentEnrollment.create(TENANT_ID, null, PROGRAM_ID, LEVEL, CREATED_BY))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Student id must not be null");
        }

        @Test
        @DisplayName("should reject null programId")
        void shouldRejectNullProgramId() {
            assertThatThrownBy(() -> StudentEnrollment.create(TENANT_ID, STUDENT_ID, null, LEVEL, CREATED_BY))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Program id must not be null");
        }

        @Test
        @DisplayName("should reject null level")
        void shouldRejectNullLevel() {
            assertThatThrownBy(() -> StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, null, CREATED_BY))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Level must not be null");
        }

        @Test
        @DisplayName("should reject null createdBy")
        void shouldRejectNullCreatedBy() {
            assertThatThrownBy(() -> StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Created by must not be null");
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class DeactivateTests {

        private StudentEnrollment createActiveEnrollment() {
            StudentEnrollment enrollment = StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY);
            enrollment.clearDomainEvents();
            return enrollment;
        }

        @Test
        @DisplayName("should transition to INACTIVE status")
        void shouldTransitionToInactive() {
            StudentEnrollment enrollment = createActiveEnrollment();
            UUID deactivatedBy = UUID.randomUUID();

            enrollment.deactivate(deactivatedBy);

            assertThat(enrollment.getStatus()).isEqualTo("INACTIVE");
            assertThat(enrollment.getUpdatedAt()).isNotNull();
            assertThat(enrollment.getUpdatedBy()).isEqualTo(deactivatedBy);
        }

        @Test
        @DisplayName("should throw IllegalStateException if already INACTIVE")
        void shouldThrowWhenAlreadyInactive() {
            StudentEnrollment enrollment = createActiveEnrollment();
            UUID deactivatedBy = UUID.randomUUID();

            enrollment.deactivate(deactivatedBy);

            assertThatThrownBy(() -> enrollment.deactivate(deactivatedBy))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Enrollment is already inactive");
        }
    }

    @Nested
    @DisplayName("Domain events")
    class DomainEvents {

        @Test
        @DisplayName("should return unmodifiable list from getDomainEvents")
        void shouldReturnUnmodifiableList() {
            StudentEnrollment enrollment = StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY);

            List<DomainEvent> events = enrollment.getDomainEvents();
            assertThatThrownBy(events::clear).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should clear all domain events when clearDomainEvents is called")
        void shouldClearDomainEvents() {
            StudentEnrollment enrollment = StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY);

            enrollment.clearDomainEvents();

            assertThat(enrollment.getDomainEvents()).isEmpty();
        }
    }
}
