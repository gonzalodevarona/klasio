package com.klasio.attendance.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the two new walk-in query methods on
 * {@link JpaAttendanceRegistrationRepository}:
 * <ul>
 *   <li>{@code findActiveBySessionAndStudent}</li>
 *   <li>{@code findActiveStudentIdsBySession}</li>
 * </ul>
 *
 * The fixture inserts the minimum required FK parents via native SQL so that
 * attendance_registrations rows can be inserted without violating constraints.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAttendanceRegistrationRepository.class, AttendanceRegistrationMapper.class})
class JpaAttendanceRegistrationRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("klasio_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JpaAttendanceRegistrationRepository repository;

    @Autowired
    private EntityManager em;

    // Fixed IDs shared across all fixture rows
    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CLASS_ID   = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID PLAN_ID    = UUID.randomUUID();
    private static final UUID ACTOR_ID   = UUID.randomUUID();

    @BeforeEach
    void seedFixtures() {
        // Apply RLS context so JPA queries and policies see the test tenant
        em.createNativeQuery(
                "SELECT set_config('app.current_tenant', :tenantId, false)")
                .setParameter("tenantId", TENANT_ID.toString())
                .getSingleResult();

        // tenant
        em.createNativeQuery("""
                INSERT INTO tenants (id, name, discipline, slug,
                  contact_email, contact_phone,
                  contact_street, contact_city, contact_state, contact_country,
                  contact_phone_indicator, language, status, created_at, created_by)
                VALUES (:id, 'Test League', 'Football', :slug,
                        'test@league.com', '3001234567',
                        'Calle 1', 'Bogota', 'Cundinamarca', 'Colombia',
                        '57', 'es', 'ACTIVE', NOW(), :createdBy)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", TENANT_ID)
                .setParameter("slug", "test-league-" + TENANT_ID.toString().substring(0, 8))
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        // program
        em.createNativeQuery("""
                INSERT INTO programs (id, tenant_id, name, status, created_at, created_by)
                VALUES (:id, :tenantId, 'Kids Football', 'ACTIVE', NOW(), :createdBy)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", PROGRAM_ID)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        // program_plan
        em.createNativeQuery("""
                INSERT INTO program_plans (id, program_id, tenant_id, name, modality,
                  manager_id, cost, hours, status, created_at, created_by)
                VALUES (:id, :programId, :tenantId, 'Basic Plan', 'HOURS_BASED',
                        :managerId, 100000, 8, 'ACTIVE', NOW(), :createdBy)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", PLAN_ID)
                .setParameter("programId", PROGRAM_ID)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("managerId", ACTOR_ID)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        // program_class
        em.createNativeQuery("""
                INSERT INTO program_classes (id, tenant_id, program_id, name,
                  level, type, max_students,
                  status, created_at, created_by)
                VALUES (:id, :tenantId, :programId, 'Monday Kids',
                        'BEGINNER', 'RECURRING', 20,
                        'ACTIVE', NOW(), :createdBy)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", CLASS_ID)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("programId", PROGRAM_ID)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        // class_session
        em.createNativeQuery("""
                INSERT INTO class_sessions (id, tenant_id, class_id,
                  session_date, start_time, end_time,
                  status, created_at, created_by)
                VALUES (:id, :tenantId, :classId,
                        '2026-05-05', '08:00', '10:00',
                        'SCHEDULED', NOW(), :createdBy)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", SESSION_ID)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("classId", CLASS_ID)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();
    }

    // ---------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------

    /** Inserts a user + student row (no FK to user_id required since user_id is nullable). */
    private UUID insertStudent() {
        UUID studentId = UUID.randomUUID();
        String uniqueEmail = studentId + "@test.com";
        String uniqueIdNumber = studentId.toString().substring(0, 8);

        em.createNativeQuery("""
                INSERT INTO students (id, tenant_id, first_name, last_name,
                  email, identity_document_type, identity_number, date_of_birth, eps,
                  status, created_at, created_by)
                VALUES (:id, :tenantId, 'John', 'Doe',
                        :email, 'CC', :idNumber, '2000-01-01', 'Sura',
                        'ACTIVE', NOW(), :createdBy)
                """)
                .setParameter("id", studentId)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("email", uniqueEmail)
                .setParameter("idNumber", uniqueIdNumber)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        return studentId;
    }

    /** Inserts a student_enrollment for the given student. */
    private UUID insertEnrollment(UUID studentId) {
        UUID enrollmentId = UUID.randomUUID();

        em.createNativeQuery("""
                INSERT INTO student_enrollments (id, tenant_id, student_id, program_id,
                  level, enrollment_date, status, created_at, created_by)
                VALUES (:id, :tenantId, :studentId, :programId,
                        'BEGINNER', '2026-01-01', 'ACTIVE', NOW(), :createdBy)
                """)
                .setParameter("id", enrollmentId)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("studentId", studentId)
                .setParameter("programId", PROGRAM_ID)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        return enrollmentId;
    }

    /** Inserts an ACTIVE membership for the given student + enrollment. */
    private UUID insertMembership(UUID studentId, UUID enrollmentId) {
        UUID membershipId = UUID.randomUUID();

        em.createNativeQuery("""
                INSERT INTO memberships (id, tenant_id, student_id, enrollment_id, program_id,
                  plan_id, plan_name, purchased_hours, available_hours,
                  start_date, expiration_date, status, created_at, created_by)
                VALUES (:id, :tenantId, :studentId, :enrollmentId, :programId,
                        :planId, 'Basic Plan', 8, 8,
                        '2026-05-01', '2026-05-31', 'ACTIVE', NOW(), :createdBy)
                """)
                .setParameter("id", membershipId)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("studentId", studentId)
                .setParameter("enrollmentId", enrollmentId)
                .setParameter("programId", PROGRAM_ID)
                .setParameter("planId", PLAN_ID)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        return membershipId;
    }

    /** Inserts a REGISTERED attendance registration row. */
    private UUID insertRegistration(UUID studentId, UUID enrollmentId,
                                    UUID membershipId, UUID sessionId) {
        UUID regId = UUID.randomUUID();

        em.createNativeQuery("""
                INSERT INTO attendance_registrations
                  (id, tenant_id, session_id, class_id, student_id,
                   enrollment_id, membership_id, level_at_registration, intended_hours,
                   status, session_date, session_start_time, session_end_time,
                   created_at, created_by)
                VALUES (:id, :tenantId, :sessionId, :classId, :studentId,
                        :enrollmentId, :membershipId, 'BEGINNER', 1,
                        'REGISTERED', '2026-05-05', '08:00', '10:00',
                        NOW(), :createdBy)
                """)
                .setParameter("id", regId)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("sessionId", sessionId)
                .setParameter("classId", CLASS_ID)
                .setParameter("studentId", studentId)
                .setParameter("enrollmentId", enrollmentId)
                .setParameter("membershipId", membershipId)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        return regId;
    }

    /** Updates the status of an existing registration row directly. */
    private void updateStatus(UUID regId, String newStatus) {
        em.createNativeQuery(
                "UPDATE attendance_registrations SET status = :status WHERE id = :id")
                .setParameter("status", newStatus)
                .setParameter("id", regId)
                .executeUpdate();
    }

    // ---------------------------------------------------------------
    // findActiveBySessionAndStudent
    // ---------------------------------------------------------------

    @Test
    @DisplayName("findActiveBySessionAndStudent returns the row when status is REGISTERED")
    void findActiveBySessionAndStudent_returnsRow_whenRegistered() {
        UUID studentId    = insertStudent();
        UUID enrollmentId = insertEnrollment(studentId);
        UUID membershipId = insertMembership(studentId, enrollmentId);
        insertRegistration(studentId, enrollmentId, membershipId, SESSION_ID);

        Optional<com.klasio.attendance.domain.model.AttendanceRegistration> result =
                repository.findActiveBySessionAndStudent(TENANT_ID, SESSION_ID, studentId);

        assertThat(result).isPresent();
        assertThat(result.get().getStudentId()).isEqualTo(studentId);
        assertThat(result.get().getStatus().name()).isEqualTo("REGISTERED");
    }

    @Test
    @DisplayName("findActiveBySessionAndStudent returns the row when status is PRESENT")
    void findActiveBySessionAndStudent_returnsRow_whenPresent() {
        UUID studentId    = insertStudent();
        UUID enrollmentId = insertEnrollment(studentId);
        UUID membershipId = insertMembership(studentId, enrollmentId);
        UUID regId        = insertRegistration(studentId, enrollmentId, membershipId, SESSION_ID);
        updateStatus(regId, "PRESENT");

        Optional<com.klasio.attendance.domain.model.AttendanceRegistration> result =
                repository.findActiveBySessionAndStudent(TENANT_ID, SESSION_ID, studentId);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus().name()).isEqualTo("PRESENT");
    }

    @Test
    @DisplayName("findActiveBySessionAndStudent returns empty when status is CANCELLED_BY_STUDENT")
    void findActiveBySessionAndStudent_returnsEmpty_whenCancelled() {
        UUID studentId    = insertStudent();
        UUID enrollmentId = insertEnrollment(studentId);
        UUID membershipId = insertMembership(studentId, enrollmentId);
        UUID regId        = insertRegistration(studentId, enrollmentId, membershipId, SESSION_ID);
        updateStatus(regId, "CANCELLED_BY_STUDENT");

        Optional<com.klasio.attendance.domain.model.AttendanceRegistration> result =
                repository.findActiveBySessionAndStudent(TENANT_ID, SESSION_ID, studentId);

        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------
    // findActiveStudentIdsBySession
    // ---------------------------------------------------------------

    @Test
    @DisplayName("findActiveStudentIdsBySession returns only non-cancelled student IDs")
    void findActiveStudentIdsBySession_returnsOnlyNonCancelledRows() {
        // Student A: REGISTERED → included
        UUID studentA    = insertStudent();
        UUID enrollA     = insertEnrollment(studentA);
        UUID memberA     = insertMembership(studentA, enrollA);
        insertRegistration(studentA, enrollA, memberA, SESSION_ID);

        // Student B: PRESENT → included
        UUID studentB    = insertStudent();
        UUID enrollB     = insertEnrollment(studentB);
        UUID memberB     = insertMembership(studentB, enrollB);
        UUID regB        = insertRegistration(studentB, enrollB, memberB, SESSION_ID);
        updateStatus(regB, "PRESENT");

        // Student C: SESSION_CANCELLED → excluded
        UUID studentC    = insertStudent();
        UUID enrollC     = insertEnrollment(studentC);
        UUID memberC     = insertMembership(studentC, enrollC);
        UUID regC        = insertRegistration(studentC, enrollC, memberC, SESSION_ID);
        updateStatus(regC, "SESSION_CANCELLED");

        Set<UUID> result = repository.findActiveStudentIdsBySession(TENANT_ID, SESSION_ID);

        assertThat(result).containsExactlyInAnyOrder(studentA, studentB);
        assertThat(result).doesNotContain(studentC);
    }
}
