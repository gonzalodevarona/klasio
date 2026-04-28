package com.klasio.programclass.infrastructure.web;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for GET /api/v1/me/classes (RF-36).
 *
 * <p>Verifies that a student enrolled at BEGINNER level in program P1 sees:
 * <ul>
 *   <li>BEGINNER-level classes for P1 — included</li>
 *   <li>OPEN-level classes for P1 — included</li>
 *   <li>INTERMEDIATE-level classes for P1 — excluded</li>
 *   <li>ADVANCED-level classes for P1 — excluded</li>
 *   <li>No duplicate class IDs in the response</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeClassesControllerIT {

    // ─── Testcontainers ──────────────────────────────────────────────────────

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("klasio_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("klasio.jwt.secret", () ->
                "test-secret-key-for-unit-tests-minimum-256-bits-long-for-hs256");
    }

    // ─── Spring beans ────────────────────────────────────────────────────────

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    // ─── Fixed identifiers ───────────────────────────────────────────────────

    private static final UUID TENANT_ID    = UUID.randomUUID();
    private static final UUID PROGRAM_ID   = UUID.randomUUID();
    private static final UUID PLAN_ID      = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();

    /** The student user whose JWT is used when calling GET /me/classes. */
    private static final UUID STUDENT_USER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID      = UUID.randomUUID();

    /** Class IDs created in @BeforeEach. */
    private static final UUID CLASS_BEGINNER_ID     = UUID.randomUUID();
    private static final UUID CLASS_INTERMEDIATE_ID = UUID.randomUUID();
    private static final UUID CLASS_ADVANCED_ID     = UUID.randomUUID();
    private static final UUID CLASS_OPEN_ID         = UUID.randomUUID();

    // ─── JWT ─────────────────────────────────────────────────────────────────

    private static final String JWT_SECRET =
            "test-secret-key-for-unit-tests-minimum-256-bits-long-for-hs256";

    /** Student JWT — the principal used for GET /me/classes. */
    private String studentJwt() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(STUDENT_USER_ID.toString())
                .claim("tenant_id", TENANT_ID.toString())
                .claim("roles", List.of("STUDENT"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    // ─── Fixtures ────────────────────────────────────────────────────────────

    @BeforeEach
    void seedFixtures() {
        jdbc.execute("SELECT set_config('app.current_tenant', '" + TENANT_ID + "', false)");

        insertTenant();
        insertProgram();
        insertPlan();

        // The student user must exist in the users table so StudentIdAdapter can resolve it
        insertUser(STUDENT_USER_ID, STUDENT_USER_ID + "@student.com");

        // Seed the student linked to that user, enrolled at BEGINNER in PROGRAM_ID
        insertStudentWithEnrollment(STUDENT_ID, STUDENT_USER_ID, "BEGINNER");

        // 4 classes: one per level, each with a non-conflicting schedule day
        insertClass(CLASS_BEGINNER_ID,     "Beginner Practice",     "BEGINNER",     "MONDAY");
        insertClass(CLASS_INTERMEDIATE_ID, "Intermediate Practice", "INTERMEDIATE", "TUESDAY");
        insertClass(CLASS_ADVANCED_ID,     "Advanced Practice",     "ADVANCED",     "WEDNESDAY");
        insertClass(CLASS_OPEN_ID,         "Open Practice",         "OPEN",         "THURSDAY");
    }

    @AfterEach
    void cleanUp() {
        jdbc.execute("SELECT set_config('app.current_tenant', '" + TENANT_ID + "', false)");
        // Delete in reverse FK order
        jdbc.execute("DELETE FROM class_schedule_entries WHERE class_id IN ('"
                + CLASS_BEGINNER_ID + "','"
                + CLASS_INTERMEDIATE_ID + "','"
                + CLASS_ADVANCED_ID + "','"
                + CLASS_OPEN_ID + "')");
        jdbc.execute("DELETE FROM program_classes WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM student_enrollments WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM students WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM users WHERE id = '" + STUDENT_USER_ID + "'");
        jdbc.execute("DELETE FROM program_plans WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM programs WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM tenants WHERE id = '" + TENANT_ID + "'");
    }

    // ─── Test ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me/classes returns BEGINNER+OPEN classes, excludes INTERMEDIATE+ADVANCED, no duplicates")
    void mergesEnrollmentLevelAndOpenClassesAndExcludesOtherLevels() throws Exception {
        mockMvc.perform(
                        get("/api/v1/me/classes")
                                .header("Authorization", "Bearer " + studentJwt()))
                .andExpect(status().isOk())
                // Exactly 2 classes must be returned: BEGINNER + OPEN
                .andExpect(jsonPath("$", hasSize(2)))
                // BEGINNER class must be present
                .andExpect(jsonPath("$[*].id", hasItem(CLASS_BEGINNER_ID.toString())))
                // OPEN class must be present
                .andExpect(jsonPath("$[*].id", hasItem(CLASS_OPEN_ID.toString())))
                // INTERMEDIATE class must NOT be present
                .andExpect(jsonPath("$[*].id", not(hasItem(CLASS_INTERMEDIATE_ID.toString()))))
                // ADVANCED class must NOT be present
                .andExpect(jsonPath("$[*].id", not(hasItem(CLASS_ADVANCED_ID.toString()))));
    }

    // ─── Fixture helpers ─────────────────────────────────────────────────────

    private void insertTenant() {
        jdbc.update("""
                INSERT INTO tenants (id, name, discipline, slug,
                  contact_email, contact_phone,
                  contact_street, contact_city, contact_state, contact_country,
                  contact_phone_indicator, language, status, created_at, created_by)
                VALUES (?, 'IT League', 'Jiu-Jitsu', ?,
                        'it@league.com', '3001234567',
                        'Calle 1', 'Bogota', 'Cundinamarca', 'Colombia',
                        '57', 'en', 'ACTIVE', NOW(), ?)
                ON CONFLICT (id) DO NOTHING
                """,
                TENANT_ID,
                "it-league-" + TENANT_ID.toString().substring(0, 8),
                ADMIN_USER_ID);
    }

    private void insertProgram() {
        jdbc.update("""
                INSERT INTO programs (id, tenant_id, name, status, created_at, created_by)
                VALUES (?, ?, 'Adults', 'ACTIVE', NOW(), ?)
                ON CONFLICT (id) DO NOTHING
                """,
                PROGRAM_ID, TENANT_ID, ADMIN_USER_ID);
    }

    private void insertPlan() {
        jdbc.update("""
                INSERT INTO program_plans (id, program_id, tenant_id, name, modality,
                  manager_id, cost, hours, status, created_at, created_by)
                VALUES (?, ?, ?, 'Basic Plan', 'HOURS_BASED',
                        ?, 100000, 8, 'ACTIVE', NOW(), ?)
                ON CONFLICT (id) DO NOTHING
                """,
                PLAN_ID, PROGRAM_ID, TENANT_ID, ADMIN_USER_ID, ADMIN_USER_ID);
    }

    private void insertUser(UUID userId, String email) {
        String idNumber = userId.toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO users (id, email, identity_document_type, identity_number, status, created_at, updated_at)
                VALUES (?, ?, 'CC', ?, 'ACTIVE', NOW(), NOW())
                ON CONFLICT (id) DO NOTHING
                """,
                userId, email, idNumber);
    }

    /**
     * Inserts a student row linked to {@code userId} and an active enrollment in PROGRAM_ID
     * at the given {@code level}.
     */
    private void insertStudentWithEnrollment(UUID studentId, UUID userId, String level) {
        String email     = studentId + "@student.com";
        String idNumber  = studentId.toString().substring(0, 8);

        jdbc.update("""
                INSERT INTO students (id, tenant_id, first_name, last_name,
                  email, identity_document_type, identity_number, date_of_birth, eps,
                  user_id, status, created_at, created_by)
                VALUES (?, ?, 'Test', 'Student',
                        ?, 'CC', ?, '2000-01-01', 'Sura',
                        ?, 'ACTIVE', NOW(), ?)
                """,
                studentId, TENANT_ID,
                email, idNumber,
                userId, ADMIN_USER_ID);

        UUID enrollmentId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO student_enrollments (id, tenant_id, student_id, program_id,
                  level, enrollment_date, status, created_at, created_by)
                VALUES (?, ?, ?, ?,
                        ?, '2026-01-01', 'ACTIVE', NOW(), ?)
                """,
                enrollmentId, TENANT_ID, studentId, PROGRAM_ID,
                level, ADMIN_USER_ID);
    }

    /**
     * Inserts a program class with a single schedule entry on the given day of week.
     * Each class uses a distinct day to avoid any unique-constraint conflicts.
     */
    private void insertClass(UUID classId, String name, String level, String dayOfWeek) {
        jdbc.update("""
                INSERT INTO program_classes (id, tenant_id, program_id, name,
                  level, type, max_students, status, created_at, created_by)
                VALUES (?, ?, ?, ?,
                        ?, 'RECURRING', 20, 'ACTIVE', NOW(), ?)
                ON CONFLICT (id) DO NOTHING
                """,
                classId, TENANT_ID, PROGRAM_ID, name, level, ADMIN_USER_ID);

        UUID scheduleEntryId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO class_schedule_entries (id, class_id, tenant_id,
                  day_of_week, specific_date, start_time, end_time)
                VALUES (?, ?, ?,
                        ?, NULL, '09:00', '11:00')
                ON CONFLICT (id) DO NOTHING
                """,
                scheduleEntryId, classId, TENANT_ID, dayOfWeek);
    }
}
