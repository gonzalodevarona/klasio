package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.model.AttendanceRegistration;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JpaAttendanceRegistrationRepository#findFutureRegisteredForClass}.
 *
 * Fixture layout:
 * <ul>
 *   <li>futureSession    — session_date 2099-01-10, start_time 08:00 → clearly future</li>
 *   <li>pastSession      — session_date 2000-01-10, start_time 08:00 → clearly past</li>
 * </ul>
 * Three registrations are inserted:
 * <ol>
 *   <li>REGISTERED on futureSession  → must be returned</li>
 *   <li>REGISTERED on pastSession    → must NOT be returned (past)</li>
 *   <li>PRESENT   on futureSession   → must NOT be returned (wrong status)</li>
 * </ol>
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAttendanceRegistrationRepository.class, AttendanceRegistrationMapper.class})
class JpaAttendanceRegistrationFutureQueryIT {

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

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CLASS_ID   = UUID.randomUUID();
    private static final UUID PLAN_ID    = UUID.randomUUID();
    private static final UUID ACTOR_ID   = UUID.randomUUID();

    private UUID futureSessionId;
    private UUID pastSessionId;

    @BeforeEach
    void seedFixtures() {
        em.createNativeQuery(
                "SELECT set_config('app.current_tenant', :tenantId, false)")
                .setParameter("tenantId", TENANT_ID.toString())
                .getSingleResult();

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

        em.createNativeQuery("""
                INSERT INTO programs (id, tenant_id, name, status, created_at, created_by)
                VALUES (:id, :tenantId, 'Kids Football', 'ACTIVE', NOW(), :createdBy)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", PROGRAM_ID)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

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

        em.createNativeQuery("""
                INSERT INTO program_classes (id, tenant_id, program_id, name,
                  level, type, max_students, status, created_at, created_by)
                VALUES (:id, :tenantId, :programId, 'Open Class',
                        'OPEN', 'RECURRING', 20, 'ACTIVE', NOW(), :createdBy)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", CLASS_ID)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("programId", PROGRAM_ID)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        futureSessionId = insertSession("2099-01-10", "08:00", "10:00");
        pastSessionId   = insertSession("2000-01-10", "08:00", "10:00");
    }

    // ---------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------

    @Test
    @DisplayName("returns only the future REGISTERED registration — excludes past and non-REGISTERED")
    void findFutureRegisteredForClass_returnsOnlyFutureRegistered() {
        // future REGISTERED — must be in result
        UUID studentA    = insertStudent();
        UUID enrollA     = insertEnrollment(studentA);
        UUID memberA     = insertMembership(studentA, enrollA);
        UUID futureRegId = insertRegistration(studentA, enrollA, memberA, futureSessionId,
                "2099-01-10", "REGISTERED");

        // past REGISTERED — must NOT be in result
        UUID studentB = insertStudent();
        UUID enrollB  = insertEnrollment(studentB);
        UUID memberB  = insertMembership(studentB, enrollB);
        insertRegistration(studentB, enrollB, memberB, pastSessionId, "2000-01-10", "REGISTERED");

        // future PRESENT — must NOT be in result
        UUID studentC    = insertStudent();
        UUID enrollC     = insertEnrollment(studentC);
        UUID memberC     = insertMembership(studentC, enrollC);
        UUID futurePresId = insertRegistration(studentC, enrollC, memberC, futureSessionId,
                "2099-01-10", "REGISTERED");
        updateStatus(futurePresId, "PRESENT");

        Instant now = Instant.now();
        List<AttendanceRegistration> result =
                repository.findFutureRegisteredForClass(TENANT_ID, CLASS_ID, now);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId().value()).isEqualTo(futureRegId);
        assertThat(result.get(0).getStatus().name()).isEqualTo("REGISTERED");
    }

    @Test
    @DisplayName("returns empty list when all REGISTERED registrations are in the past")
    void findFutureRegisteredForClass_returnsEmpty_whenAllPast() {
        UUID studentA = insertStudent();
        UUID enrollA  = insertEnrollment(studentA);
        UUID memberA  = insertMembership(studentA, enrollA);
        insertRegistration(studentA, enrollA, memberA, pastSessionId, "2000-01-10", "REGISTERED");

        List<AttendanceRegistration> result =
                repository.findFutureRegisteredForClass(TENANT_ID, CLASS_ID, Instant.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty list when there are no registrations for the class")
    void findFutureRegisteredForClass_returnsEmpty_whenNoRegistrations() {
        List<AttendanceRegistration> result =
                repository.findFutureRegisteredForClass(TENANT_ID, CLASS_ID, Instant.now());

        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------

    private UUID insertSession(String date, String startTime, String endTime) {
        UUID sessionId = UUID.randomUUID();
        em.createNativeQuery("""
                INSERT INTO class_sessions (id, tenant_id, class_id,
                  session_date, start_time, end_time,
                  status, created_at, created_by)
                VALUES (:id, :tenantId, :classId,
                        CAST(:date AS DATE), CAST(:startTime AS TIME), CAST(:endTime AS TIME),
                        'SCHEDULED', NOW(), :createdBy)
                ON CONFLICT (class_id, session_date, start_time) DO NOTHING
                """)
                .setParameter("id", sessionId)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("classId", CLASS_ID)
                .setParameter("date", date)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();
        return sessionId;
    }

    private UUID insertStudent() {
        UUID studentId = UUID.randomUUID();
        String uniqueEmail    = studentId + "@test.com";
        String uniqueIdNumber = studentId.toString().substring(0, 8);

        em.createNativeQuery("""
                INSERT INTO students (id, tenant_id, first_name, last_name,
                  email, identity_document_type, identity_number, date_of_birth, eps,
                  status, created_at, created_by)
                VALUES (:id, :tenantId, 'Jane', 'Doe',
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

    private UUID insertMembership(UUID studentId, UUID enrollmentId) {
        UUID membershipId = UUID.randomUUID();
        em.createNativeQuery("""
                INSERT INTO memberships (id, tenant_id, student_id, enrollment_id, program_id,
                  plan_id, plan_name, modality, purchased_hours, available_hours,
                  start_date, expiration_date, status, created_at, created_by)
                VALUES (:id, :tenantId, :studentId, :enrollmentId, :programId,
                        :planId, 'Basic Plan', 'HOURS_BASED', 8, 8,
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

    private UUID insertRegistration(UUID studentId, UUID enrollmentId, UUID membershipId,
                                    UUID sessionId, String sessionDate, String status) {
        UUID regId = UUID.randomUUID();
        em.createNativeQuery("""
                INSERT INTO attendance_registrations
                  (id, tenant_id, session_id, class_id, student_id,
                   enrollment_id, membership_id, level_at_registration, intended_hours,
                   status, session_date, session_start_time, session_end_time,
                   created_at, created_by)
                VALUES (:id, :tenantId, :sessionId, :classId, :studentId,
                        :enrollmentId, :membershipId, 'BEGINNER', 1,
                        :status, CAST(:sessionDate AS DATE), '08:00', '10:00',
                        NOW(), :createdBy)
                """)
                .setParameter("id", regId)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("sessionId", sessionId)
                .setParameter("classId", CLASS_ID)
                .setParameter("studentId", studentId)
                .setParameter("enrollmentId", enrollmentId)
                .setParameter("membershipId", membershipId)
                .setParameter("status", status)
                .setParameter("sessionDate", sessionDate)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();
        return regId;
    }

    private void updateStatus(UUID regId, String newStatus) {
        em.createNativeQuery(
                "UPDATE attendance_registrations SET status = :status WHERE id = :id")
                .setParameter("status", newStatus)
                .setParameter("id", regId)
                .executeUpdate();
    }
}
