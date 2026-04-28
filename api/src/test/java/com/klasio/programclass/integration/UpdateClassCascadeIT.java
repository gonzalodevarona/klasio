package com.klasio.programclass.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test verifying that changing a class from OPEN to a specific
 * level (e.g., BEGINNER) cancels future registrations for mismatching students,
 * leaves matching registrations intact, ignores past PRESENT rows, and
 * writes both audit_log and notification rows for affected students (RF-36).
 *
 * <p>Fixture layout:
 * <ul>
 *   <li>1 class with level=OPEN and 1 future session</li>
 *   <li>3 students: BEGINNER, INTERMEDIATE, ADVANCED — each with an enrollment,
 *       active membership, and a future REGISTERED attendance row</li>
 *   <li>1 past PRESENT row (for the BEGINNER student) — must not be touched</li>
 * </ul>
 *
 * <p>After PUT /api/v1/programs/{programId}/classes/{classId} with level=BEGINNER:
 * <ul>
 *   <li>BEGINNER registration → remains REGISTERED</li>
 *   <li>INTERMEDIATE registration → becomes CANCELLED_BY_SYSTEM</li>
 *   <li>ADVANCED registration → becomes CANCELLED_BY_SYSTEM</li>
 *   <li>Past PRESENT row → unchanged</li>
 *   <li>2 audit_log rows with action_type=ATTENDANCE_REGISTRATION_CANCELLED_BY_LEVEL_CHANGE</li>
 *   <li>2 notification rows with type=CLASS_LEVEL_CHANGED (one per affected student)</li>
 * </ul>
 *
 * <p>NOTE: The test does NOT annotate the class or method with {@code @Transactional}.
 * That would prevent the outer transaction from committing, which would silence the
 * {@code @TransactionalEventListener(AFTER_COMMIT)} in {@code LevelChangeNotificationListener}.
 * Data cleanup happens in {@code @AfterEach} via DELETE statements.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UpdateClassCascadeIT {

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
        // Use the test JWT secret defined in src/test/resources/application.yml
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
    private static final UUID CLASS_ID     = UUID.randomUUID();
    private static final UUID SESSION_ID   = UUID.randomUUID();

    /** The admin user that calls PUT /classes/{id}. */
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();

    /** Students and their linked users. */
    private static final UUID STUDENT_BEGINNER_ID      = UUID.randomUUID();
    private static final UUID STUDENT_INTERMEDIATE_ID  = UUID.randomUUID();
    private static final UUID STUDENT_ADVANCED_ID      = UUID.randomUUID();

    private static final UUID USER_BEGINNER_ID     = UUID.randomUUID();
    private static final UUID USER_INTERMEDIATE_ID = UUID.randomUUID();
    private static final UUID USER_ADVANCED_ID     = UUID.randomUUID();

    /** Future registration IDs — asserted after the PUT. */
    private UUID regBeginnerId;
    private UUID regIntermediateId;
    private UUID regAdvancedId;

    /** Past PRESENT registration for the BEGINNER student — must remain PRESENT. */
    private static final UUID SESSION_PAST_ID = UUID.randomUUID();
    private UUID regPastBeginnerPresent;

    // ─── JWT ─────────────────────────────────────────────────────────────────

    private static final String JWT_SECRET =
            "test-secret-key-for-unit-tests-minimum-256-bits-long-for-hs256";

    private String adminJwt() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(ADMIN_USER_ID.toString())
                .claim("tenant_id", TENANT_ID.toString())
                .claim("roles", List.of("ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    // ─── Fixtures ────────────────────────────────────────────────────────────

    @BeforeEach
    void seedFixtures() {
        // Set RLS context for all subsequent JDBC queries in this connection
        jdbc.execute("SELECT set_config('app.current_tenant', '" + TENANT_ID + "', false)");

        insertTenant();
        insertProgram();
        insertPlan();
        insertClass();

        // Future session (2099 → definitely future)
        insertSession(SESSION_ID, "2099-06-10", "09:00", "11:00");
        // Past session (2000 → definitely past)
        insertSession(SESSION_PAST_ID, "2000-01-10", "09:00", "11:00");

        // Insert users for notifications (students need user_id so the listener can resolve them)
        insertUser(USER_BEGINNER_ID, "beginner" + TENANT_ID + "@test.com");
        insertUser(USER_INTERMEDIATE_ID, "intermediate" + TENANT_ID + "@test.com");
        insertUser(USER_ADVANCED_ID, "advanced" + TENANT_ID + "@test.com");

        // Enroll each student at a different level and give them an active membership
        UUID enrollBeginner     = enrollStudent(STUDENT_BEGINNER_ID,    USER_BEGINNER_ID,     "BEGINNER");
        UUID enrollIntermediate = enrollStudent(STUDENT_INTERMEDIATE_ID, USER_INTERMEDIATE_ID, "INTERMEDIATE");
        UUID enrollAdvanced     = enrollStudent(STUDENT_ADVANCED_ID,     USER_ADVANCED_ID,     "ADVANCED");

        UUID memBeginner     = insertMembership(STUDENT_BEGINNER_ID,    enrollBeginner);
        UUID memIntermediate = insertMembership(STUDENT_INTERMEDIATE_ID, enrollIntermediate);
        UUID memAdvanced     = insertMembership(STUDENT_ADVANCED_ID,     enrollAdvanced);

        // Future REGISTERED registrations — all use the student's enrollment level
        regBeginnerId    = insertRegistration(STUDENT_BEGINNER_ID,    enrollBeginner,     memBeginner,
                SESSION_ID, "2099-06-10", "REGISTERED", "BEGINNER");
        regIntermediateId = insertRegistration(STUDENT_INTERMEDIATE_ID, enrollIntermediate, memIntermediate,
                SESSION_ID, "2099-06-10", "REGISTERED", "INTERMEDIATE");
        regAdvancedId    = insertRegistration(STUDENT_ADVANCED_ID,    enrollAdvanced,     memAdvanced,
                SESSION_ID, "2099-06-10", "REGISTERED", "ADVANCED");

        // Past PRESENT row for the BEGINNER student — must not be touched by the cascade
        regPastBeginnerPresent = insertRegistration(STUDENT_BEGINNER_ID, enrollBeginner, memBeginner,
                SESSION_PAST_ID, "2000-01-10", "PRESENT", "BEGINNER");
    }

    @AfterEach
    void cleanUp() {
        // Re-establish RLS context (required for FORCE RLS tables like class_schedule_entries)
        jdbc.execute("SELECT set_config('app.current_tenant', '" + TENANT_ID + "', false)");
        // Delete in reverse FK order so constraints are satisfied
        jdbc.execute("DELETE FROM notifications WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM audit_log WHERE actor_id = '" + ADMIN_USER_ID + "'");
        jdbc.execute("DELETE FROM attendance_registrations WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM class_sessions WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM memberships WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM student_enrollments WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM students WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM users WHERE id IN ('"
                + USER_BEGINNER_ID + "','" + USER_INTERMEDIATE_ID + "','" + USER_ADVANCED_ID + "')");
        jdbc.execute("DELETE FROM class_schedule_entries WHERE class_id = '" + CLASS_ID + "'");
        jdbc.execute("DELETE FROM program_classes WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM program_plans WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM programs WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM tenants WHERE id = '" + TENANT_ID + "'");
    }

    // ─── Main test ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /classes/{id} OPEN→BEGINNER cancels INTERMEDIATE+ADVANCED future regs, keeps BEGINNER, ignores past PRESENT, writes audit+notification rows")
    void openToBeginner_cancelsOnlyMismatchingFutureRegs() throws Exception {

        String requestBody = """
                {
                  "name": "Open Class",
                  "level": "BEGINNER",
                  "scheduleEntries": [
                    { "dayOfWeek": "MONDAY", "startTime": "09:00", "endTime": "11:00" }
                  ],
                  "maxStudents": 20
                }
                """;

        // ── 1. Call PUT /api/v1/programs/{programId}/classes/{classId} ────────
        mockMvc.perform(
                put("/api/v1/programs/{programId}/classes/{classId}", PROGRAM_ID, CLASS_ID)
                        .header("Authorization", "Bearer " + adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // ── 2. Assert registration statuses ───────────────────────────────────
        String statusBeginner = queryRegistrationStatus(regBeginnerId);
        assertThat(statusBeginner)
                .as("BEGINNER registration must remain REGISTERED (level matches new class level)")
                .isEqualTo("REGISTERED");

        String statusIntermediate = queryRegistrationStatus(regIntermediateId);
        assertThat(statusIntermediate)
                .as("INTERMEDIATE registration must be cancelled (level no longer matches)")
                .isEqualTo("CANCELLED_BY_SYSTEM");

        String statusAdvanced = queryRegistrationStatus(regAdvancedId);
        assertThat(statusAdvanced)
                .as("ADVANCED registration must be cancelled (level no longer matches)")
                .isEqualTo("CANCELLED_BY_SYSTEM");

        // ── 3. Past PRESENT row must not be touched ───────────────────────────
        String statusPastPresent = queryRegistrationStatus(regPastBeginnerPresent);
        assertThat(statusPastPresent)
                .as("Past PRESENT row must remain PRESENT (only future REGISTERED rows are considered)")
                .isEqualTo("PRESENT");

        // ── 4. Audit log: exactly 2 rows for the level-change cancellation ────
        int auditCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log "
                        + "WHERE action_type = 'ATTENDANCE_REGISTRATION_CANCELLED_BY_LEVEL_CHANGE' "
                        + "AND actor_id = ?",
                Integer.class,
                ADMIN_USER_ID);
        assertThat(auditCount)
                .as("Expected 2 audit_log rows — one for INTERMEDIATE, one for ADVANCED")
                .isEqualTo(2);

        // ── 5. Notifications: exactly 2 rows (one per affected student) ───────
        // The listener fires @TransactionalEventListener(AFTER_COMMIT) (not @Async),
        // so by the time MockMvc returns these rows must already be committed.
        // The notifications table has FORCE ROW LEVEL SECURITY — must set app.current_tenant
        // on the same JDBC connection used for the query. We use execute(ConnectionCallback)
        // to pin both the set_config and the SELECT to the same pooled connection.
        int notificationCount = jdbc.execute((java.sql.Connection conn) -> {
            // The notifications table has FORCE ROW LEVEL SECURITY — pin set_config and
            // SELECT to the same pooled connection so the RLS context is active for the query.
            try (var setCtx = conn.prepareStatement(
                    "SELECT set_config('app.current_tenant', ?, false)")) {
                setCtx.setString(1, TENANT_ID.toString());
                setCtx.execute();
            }
            try (var ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM notifications WHERE tenant_id = ? AND type = 'CLASS_LEVEL_CHANGED'")) {
                ps.setObject(1, TENANT_ID);
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
        assertThat(notificationCount)
                .as("Expected 2 notifications — one for INTERMEDIATE, one for ADVANCED students")
                .isEqualTo(2);
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

    private void insertClass() {
        jdbc.update("""
                INSERT INTO program_classes (id, tenant_id, program_id, name,
                  level, type, max_students, status, created_at, created_by)
                VALUES (?, ?, ?, 'Open Class',
                        'OPEN', 'RECURRING', 20, 'ACTIVE', NOW(), ?)
                ON CONFLICT (id) DO NOTHING
                """,
                CLASS_ID, TENANT_ID, PROGRAM_ID, ADMIN_USER_ID);

        // Insert the same schedule entry that the PUT request will send (MONDAY 09:00–11:00).
        // The scheduleChanged detection in ProgramClass.update() compares the incoming list
        // against the persisted one — if they match, scheduleChanged=false and the
        // ClassScheduleChangedListener will skip the cancellation, leaving only the
        // level-change cascade to run.
        UUID scheduleId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO class_schedule_entries (id, class_id, tenant_id,
                  day_of_week, specific_date, start_time, end_time)
                VALUES (?, ?, ?,
                        'MONDAY', NULL, '09:00', '11:00')
                ON CONFLICT (id) DO NOTHING
                """,
                scheduleId, CLASS_ID, TENANT_ID);
    }

    private void insertSession(UUID sessionId, String date, String startTime, String endTime) {
        jdbc.update("""
                INSERT INTO class_sessions (id, tenant_id, class_id,
                  session_date, start_time, end_time,
                  status, created_at, created_by)
                VALUES (?, ?, ?,
                        CAST(? AS DATE), CAST(? AS TIME), CAST(? AS TIME),
                        'SCHEDULED', NOW(), ?)
                ON CONFLICT (id) DO NOTHING
                """,
                sessionId, TENANT_ID, CLASS_ID, date, startTime, endTime, ADMIN_USER_ID);
    }

    /**
     * Inserts a user row and a student row linked to that user, enrolled at {@code level}.
     * Returns the enrollment ID.
     */
    private UUID enrollStudent(UUID studentId, UUID userId, String level) {
        String uniqueEmail    = userId + "@student.com";
        String uniqueIdNumber = studentId.toString().substring(0, 8);

        jdbc.update("""
                INSERT INTO students (id, tenant_id, first_name, last_name,
                  email, identity_document_type, identity_number, date_of_birth, eps,
                  user_id, status, created_at, created_by)
                VALUES (?, ?, 'Student', ?,
                        ?, 'CC', ?, '2000-01-01', 'Sura',
                        ?, 'ACTIVE', NOW(), ?)
                """,
                studentId, TENANT_ID, level,
                uniqueEmail, uniqueIdNumber,
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

        return enrollmentId;
    }

    private void insertUser(UUID userId, String email) {
        // identity_number must be unique per tenant (NULL tenant → sentinel); use first 8 chars of UUID
        String idNumber = userId.toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO users (id, email, identity_document_type, identity_number, status, created_at, updated_at)
                VALUES (?, ?, 'CC', ?, 'ACTIVE', NOW(), NOW())
                ON CONFLICT (id) DO NOTHING
                """,
                userId, email, idNumber);
    }

    private UUID insertMembership(UUID studentId, UUID enrollmentId) {
        UUID membershipId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO memberships (id, tenant_id, student_id, enrollment_id, program_id,
                  plan_id, plan_name, modality, purchased_hours, available_hours,
                  start_date, expiration_date, status, created_at, created_by)
                VALUES (?, ?, ?, ?, ?,
                        ?, 'Basic Plan', 'HOURS_BASED', 8, 8,
                        '2026-05-01', '2026-05-31', 'ACTIVE', NOW(), ?)
                """,
                membershipId, TENANT_ID, studentId, enrollmentId, PROGRAM_ID,
                PLAN_ID, ADMIN_USER_ID);
        return membershipId;
    }

    private UUID insertRegistration(UUID studentId, UUID enrollmentId, UUID membershipId,
                                    UUID sessionId, String sessionDate,
                                    String status, String level) {
        UUID regId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO attendance_registrations
                  (id, tenant_id, session_id, class_id, student_id,
                   enrollment_id, membership_id, level_at_registration, intended_hours,
                   status, session_date, session_start_time, session_end_time,
                   created_at, created_by)
                VALUES (?, ?, ?, ?, ?,
                        ?, ?, ?, 1,
                        ?, CAST(? AS DATE), '09:00', '11:00',
                        NOW(), ?)
                """,
                regId, TENANT_ID, sessionId, CLASS_ID, studentId,
                enrollmentId, membershipId, level,
                status, sessionDate, ADMIN_USER_ID);
        return regId;
    }

    private String queryRegistrationStatus(UUID registrationId) {
        return jdbc.queryForObject(
                "SELECT status FROM attendance_registrations WHERE id = ?",
                String.class,
                registrationId);
    }
}
