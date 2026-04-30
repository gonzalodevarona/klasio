package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.EligibleStudentLookupPort;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort.EligibleStudentView;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link EligibleStudentLookupAdapter}.
 *
 * Each test seeds the minimum required FK parents (tenant, program, plan) once in
 * {@code @BeforeEach} and then inserts per-test students, enrollments, and memberships
 * via native SQL — the same pattern used by {@link JpaAttendanceRegistrationRepositoryIT}.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EligibleStudentLookupAdapter.class)
class EligibleStudentLookupAdapterIT {

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
    private EligibleStudentLookupAdapter adapter;

    @Autowired
    private EntityManager em;

    private static final UUID TENANT_ID   = UUID.randomUUID();
    private static final UUID PROGRAM_ID  = UUID.randomUUID();
    private static final UUID PLAN_ID     = UUID.randomUUID();
    private static final UUID ACTOR_ID    = UUID.randomUUID();

    // A second tenant to test tenant isolation
    private static final UUID OTHER_TENANT_ID  = UUID.randomUUID();
    private static final UUID OTHER_PROGRAM_ID = UUID.randomUUID();
    private static final UUID OTHER_PLAN_ID    = UUID.randomUUID();

    @BeforeEach
    void seedFixtures() {
        // Set RLS context to main tenant
        em.createNativeQuery(
                "SELECT set_config('app.current_tenant', :tenantId, false)")
                .setParameter("tenantId", TENANT_ID.toString())
                .getSingleResult();

        // Main tenant
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

        // Other tenant (for isolation test)
        em.createNativeQuery("""
                INSERT INTO tenants (id, name, discipline, slug,
                  contact_email, contact_phone,
                  contact_street, contact_city, contact_state, contact_country,
                  contact_phone_indicator, language, status, created_at, created_by)
                VALUES (:id, 'Other League', 'Basketball', :slug,
                        'other@league.com', '3009999999',
                        'Calle 2', 'Medellin', 'Antioquia', 'Colombia',
                        '57', 'es', 'ACTIVE', NOW(), :createdBy)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", OTHER_TENANT_ID)
                .setParameter("slug", "other-league-" + OTHER_TENANT_ID.toString().substring(0, 8))
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        // Main program
        em.createNativeQuery("""
                INSERT INTO programs (id, tenant_id, name, status, created_at, created_by)
                VALUES (:id, :tenantId, 'Kids Football', 'ACTIVE', NOW(), :createdBy)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", PROGRAM_ID)
                .setParameter("tenantId", TENANT_ID)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        // Other program (different tenant)
        em.createNativeQuery("""
                INSERT INTO programs (id, tenant_id, name, status, created_at, created_by)
                VALUES (:id, :tenantId, 'Adults Basketball', 'ACTIVE', NOW(), :createdBy)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", OTHER_PROGRAM_ID)
                .setParameter("tenantId", OTHER_TENANT_ID)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        // Main plan
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

        // Other plan
        em.createNativeQuery("""
                INSERT INTO program_plans (id, program_id, tenant_id, name, modality,
                  manager_id, cost, hours, status, created_at, created_by)
                VALUES (:id, :programId, :tenantId, 'Other Plan', 'HOURS_BASED',
                        :managerId, 100000, 8, 'ACTIVE', NOW(), :createdBy)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", OTHER_PLAN_ID)
                .setParameter("programId", OTHER_PROGRAM_ID)
                .setParameter("tenantId", OTHER_TENANT_ID)
                .setParameter("managerId", ACTOR_ID)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();
    }

    // ---------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------

    private UUID insertStudent(UUID tenantId, String firstName, String lastName) {
        UUID studentId = UUID.randomUUID();
        String uniqueEmail = studentId + "@test.com";
        String uniqueIdNumber = studentId.toString().substring(0, 8);

        em.createNativeQuery("""
                INSERT INTO students (id, tenant_id, first_name, last_name,
                  email, identity_document_type, identity_number, date_of_birth, eps,
                  status, created_at, created_by)
                VALUES (:id, :tenantId, :firstName, :lastName,
                        :email, 'CC', :idNumber, '2000-01-01', 'Sura',
                        'ACTIVE', NOW(), :createdBy)
                """)
                .setParameter("id", studentId)
                .setParameter("tenantId", tenantId)
                .setParameter("firstName", firstName)
                .setParameter("lastName", lastName)
                .setParameter("email", uniqueEmail)
                .setParameter("idNumber", uniqueIdNumber)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        return studentId;
    }

    private UUID insertStudentWithIdNumber(UUID tenantId, String firstName, String lastName, String idNumber) {
        UUID studentId = UUID.randomUUID();
        String uniqueEmail = studentId + "@test.com";

        em.createNativeQuery("""
                INSERT INTO students (id, tenant_id, first_name, last_name,
                  email, identity_document_type, identity_number, date_of_birth, eps,
                  status, created_at, created_by)
                VALUES (:id, :tenantId, :firstName, :lastName,
                        :email, 'CC', :idNumber, '2000-01-01', 'Sura',
                        'ACTIVE', NOW(), :createdBy)
                """)
                .setParameter("id", studentId)
                .setParameter("tenantId", tenantId)
                .setParameter("firstName", firstName)
                .setParameter("lastName", lastName)
                .setParameter("email", uniqueEmail)
                .setParameter("idNumber", idNumber)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        return studentId;
    }

    private UUID insertEnrollment(UUID tenantId, UUID studentId, UUID programId, String level) {
        UUID enrollmentId = UUID.randomUUID();

        em.createNativeQuery("""
                INSERT INTO student_enrollments (id, tenant_id, student_id, program_id,
                  level, enrollment_date, status, created_at, created_by)
                VALUES (:id, :tenantId, :studentId, :programId,
                        :level, '2026-01-01', 'ACTIVE', NOW(), :createdBy)
                """)
                .setParameter("id", enrollmentId)
                .setParameter("tenantId", tenantId)
                .setParameter("studentId", studentId)
                .setParameter("programId", programId)
                .setParameter("level", level)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        return enrollmentId;
    }

    private UUID insertMembership(UUID tenantId, UUID studentId, UUID enrollmentId,
                                   UUID programId, UUID planId, String status, int availableHours) {
        UUID membershipId = UUID.randomUUID();

        em.createNativeQuery("""
                INSERT INTO memberships (id, tenant_id, student_id, enrollment_id, program_id,
                  plan_id, plan_name, modality, purchased_hours, available_hours,
                  start_date, expiration_date, status, created_at, created_by)
                VALUES (:id, :tenantId, :studentId, :enrollmentId, :programId,
                        :planId, 'Basic Plan', 'HOURS_BASED', 8, :availableHours,
                        '2026-05-01', '2026-05-31', :status, NOW(), :createdBy)
                """)
                .setParameter("id", membershipId)
                .setParameter("tenantId", tenantId)
                .setParameter("studentId", studentId)
                .setParameter("enrollmentId", enrollmentId)
                .setParameter("programId", programId)
                .setParameter("planId", planId)
                .setParameter("availableHours", availableHours)
                .setParameter("status", status)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        return membershipId;
    }

    private UUID insertUnlimitedMembership(UUID tenantId, UUID studentId, UUID enrollmentId,
                                            UUID programId, UUID planId, String status) {
        UUID membershipId = UUID.randomUUID();

        em.createNativeQuery("""
                INSERT INTO memberships (id, tenant_id, student_id, enrollment_id, program_id,
                  plan_id, plan_name, modality, purchased_hours, available_hours,
                  start_date, expiration_date, status, created_at, created_by)
                VALUES (:id, :tenantId, :studentId, :enrollmentId, :programId,
                        :planId, 'Unlimited Plan', 'UNLIMITED', NULL, NULL,
                        '2026-05-01', '2026-05-31', :status, NOW(), :createdBy)
                """)
                .setParameter("id", membershipId)
                .setParameter("tenantId", tenantId)
                .setParameter("studentId", studentId)
                .setParameter("enrollmentId", enrollmentId)
                .setParameter("programId", programId)
                .setParameter("planId", planId)
                .setParameter("status", status)
                .setParameter("createdBy", ACTOR_ID)
                .executeUpdate();

        return membershipId;
    }

    // ---------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------

    @Test
    @DisplayName("returns only active enrollments at the requested level with active memberships")
    void findEligible_returnsOnlyActiveEnrollmentsAtLevel_withActiveMembership() {
        // 2 BEGINNER students with ACTIVE memberships — should be returned
        UUID studentA = insertStudent(TENANT_ID, "Ana", "Lopez");
        UUID enrollA  = insertEnrollment(TENANT_ID, studentA, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, studentA, enrollA, PROGRAM_ID, PLAN_ID, "ACTIVE", 5);

        UUID studentB = insertStudent(TENANT_ID, "Bruno", "Garcia");
        UUID enrollB  = insertEnrollment(TENANT_ID, studentB, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, studentB, enrollB, PROGRAM_ID, PLAN_ID, "ACTIVE", 3);

        // ADVANCED student with ACTIVE membership — wrong level
        UUID studentC = insertStudent(TENANT_ID, "Carlos", "Ruiz");
        UUID enrollC  = insertEnrollment(TENANT_ID, studentC, PROGRAM_ID, "ADVANCED");
        insertMembership(TENANT_ID, studentC, enrollC, PROGRAM_ID, PLAN_ID, "ACTIVE", 8);

        // BEGINNER student with EXPIRED membership — wrong membership status
        UUID studentD = insertStudent(TENANT_ID, "Diana", "Torres");
        UUID enrollD  = insertEnrollment(TENANT_ID, studentD, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, studentD, enrollD, PROGRAM_ID, PLAN_ID, "EXPIRED", 8);

        List<EligibleStudentView> result = adapter.findEligible(
                TENANT_ID, PROGRAM_ID, "BEGINNER", 1, null, Set.of(), 50);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EligibleStudentView::studentId)
                .containsExactlyInAnyOrder(studentA, studentB);
    }

    @Test
    @DisplayName("excludes students whose membership available_hours is below minHours")
    void findEligible_excludesMembershipsBelowMinHours() {
        UUID student = insertStudent(TENANT_ID, "Zero", "Hours");
        UUID enroll  = insertEnrollment(TENANT_ID, student, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, student, enroll, PROGRAM_ID, PLAN_ID, "ACTIVE", 0);

        List<EligibleStudentView> result = adapter.findEligible(
                TENANT_ID, PROGRAM_ID, "BEGINNER", 1, null, Set.of(), 50);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("name filter is case-insensitive and matches partial last name")
    void findEligible_filtersByName_caseInsensitive() {
        UUID studentA = insertStudent(TENANT_ID, "Juan", "Perez");
        UUID enrollA  = insertEnrollment(TENANT_ID, studentA, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, studentA, enrollA, PROGRAM_ID, PLAN_ID, "ACTIVE", 5);

        UUID studentB = insertStudent(TENANT_ID, "Maria", "Gonzalez");
        UUID enrollB  = insertEnrollment(TENANT_ID, studentB, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, studentB, enrollB, PROGRAM_ID, PLAN_ID, "ACTIVE", 5);

        List<EligibleStudentView> result = adapter.findEligible(
                TENANT_ID, PROGRAM_ID, "BEGINNER", 1, "perez", Set.of(), 50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo(studentA);
        assertThat(result.get(0).fullName()).isEqualTo("Juan Perez");
    }

    @Test
    @DisplayName("filters by partial identity_number prefix")
    void findEligible_filtersByIdDocumentPrefix() {
        UUID studentA = insertStudentWithIdNumber(TENANT_ID, "Luis", "Mora", "12345678");
        UUID enrollA  = insertEnrollment(TENANT_ID, studentA, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, studentA, enrollA, PROGRAM_ID, PLAN_ID, "ACTIVE", 5);

        UUID studentB = insertStudentWithIdNumber(TENANT_ID, "Sofia", "Vega", "99999999");
        UUID enrollB  = insertEnrollment(TENANT_ID, studentB, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, studentB, enrollB, PROGRAM_ID, PLAN_ID, "ACTIVE", 5);

        List<EligibleStudentView> result = adapter.findEligible(
                TENANT_ID, PROGRAM_ID, "BEGINNER", 1, "12345", Set.of(), 50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo(studentA);
        assertThat(result.get(0).idDocument()).isEqualTo("12345678");
    }

    @Test
    @DisplayName("excludes students whose UUID is in excludeStudentIds")
    void findEligible_respectsExcludeStudentIds() {
        UUID studentA = insertStudent(TENANT_ID, "Include", "Me");
        UUID enrollA  = insertEnrollment(TENANT_ID, studentA, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, studentA, enrollA, PROGRAM_ID, PLAN_ID, "ACTIVE", 5);

        UUID studentB = insertStudent(TENANT_ID, "Exclude", "Me");
        UUID enrollB  = insertEnrollment(TENANT_ID, studentB, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, studentB, enrollB, PROGRAM_ID, PLAN_ID, "ACTIVE", 5);

        List<EligibleStudentView> result = adapter.findEligible(
                TENANT_ID, PROGRAM_ID, "BEGINNER", 1, null, Set.of(studentB), 50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo(studentA);
    }

    @Test
    @DisplayName("empty excludeStudentIds set must not throw (avoids Postgres empty IN() bug)")
    void findEligible_handlesEmptyExcludeSet() {
        UUID student = insertStudent(TENANT_ID, "Solo", "Student");
        UUID enroll  = insertEnrollment(TENANT_ID, student, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, student, enroll, PROGRAM_ID, PLAN_ID, "ACTIVE", 5);

        assertThatNoException().isThrownBy(() ->
                adapter.findEligible(TENANT_ID, PROGRAM_ID, "BEGINNER", 1, null, Set.of(), 50));
    }

    @Test
    @DisplayName("limit parameter caps the result size")
    void findEligible_respectsLimit() {
        UUID studentA = insertStudent(TENANT_ID, "First", "Student");
        UUID enrollA  = insertEnrollment(TENANT_ID, studentA, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, studentA, enrollA, PROGRAM_ID, PLAN_ID, "ACTIVE", 5);

        UUID studentB = insertStudent(TENANT_ID, "Second", "Student");
        UUID enrollB  = insertEnrollment(TENANT_ID, studentB, PROGRAM_ID, "BEGINNER");
        insertMembership(TENANT_ID, studentB, enrollB, PROGRAM_ID, PLAN_ID, "ACTIVE", 5);

        List<EligibleStudentView> result = adapter.findEligible(
                TENANT_ID, PROGRAM_ID, "BEGINNER", 1, null, Set.of(), 1);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("UNLIMITED membership student is returned with availableHours == -1")
    void findEligible_unlimitedMembership_returnsWithSentinelMinusOne() {
        UUID student = insertStudent(TENANT_ID, "Libre", "Ilimitada");
        UUID enroll  = insertEnrollment(TENANT_ID, student, PROGRAM_ID, "BEGINNER");
        insertUnlimitedMembership(TENANT_ID, student, enroll, PROGRAM_ID, PLAN_ID, "ACTIVE");

        List<EligibleStudentView> result = adapter.findEligible(
                TENANT_ID, PROGRAM_ID, "BEGINNER", 1, null, Set.of(), 50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo(student);
        assertThat(result.get(0).availableHours()).isEqualTo(-1);
    }

    @Test
    @DisplayName("does not return students from a different tenant")
    void findEligible_isolatesByTenant() {
        // Seed a student in OTHER_TENANT
        UUID otherStudent = insertStudent(OTHER_TENANT_ID, "Other", "Tenant");
        UUID otherEnroll  = insertEnrollment(OTHER_TENANT_ID, otherStudent, OTHER_PROGRAM_ID, "BEGINNER");

        // Must set RLS to other tenant for the insert to pass the policy
        em.createNativeQuery(
                "SELECT set_config('app.current_tenant', :tenantId, false)")
                .setParameter("tenantId", OTHER_TENANT_ID.toString())
                .getSingleResult();

        insertMembership(OTHER_TENANT_ID, otherStudent, otherEnroll,
                OTHER_PROGRAM_ID, OTHER_PLAN_ID, "ACTIVE", 5);

        // Reset RLS to main tenant for the query
        em.createNativeQuery(
                "SELECT set_config('app.current_tenant', :tenantId, false)")
                .setParameter("tenantId", TENANT_ID.toString())
                .getSingleResult();

        // Query under TENANT_ID with PROGRAM_ID — must return nothing
        List<EligibleStudentView> result = adapter.findEligible(
                TENANT_ID, PROGRAM_ID, "BEGINNER", 1, null, Set.of(), 50);

        assertThat(result).isEmpty();
    }
}
