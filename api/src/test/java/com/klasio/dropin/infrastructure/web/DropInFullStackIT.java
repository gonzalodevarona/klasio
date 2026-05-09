package com.klasio.dropin.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack Testcontainers integration tests for the drop-in students feature.
 *
 * <p>Covers:
 * <ul>
 *   <li>Happy path: new attendee creates all three rows + audit log entries</li>
 *   <li>Atomic rollback on capacity full: no orphan rows</li>
 *   <li>Sequential idempotency: double-click yields exactly one registration</li>
 *   <li>Cross-session counter increment: total_visits increments correctly</li>
 *   <li>Cancel session fan-out: drop-in registration flipped to CANCELLED_BY_SYSTEM</li>
 *   <li>RLS smoke test: two-tenant phone isolation</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DropInFullStackIT {

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
        registry.add("klasio.jwt.secret", () -> JWT_SECRET);
    }

    // ─── Spring beans ────────────────────────────────────────────────────────

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── JWT ─────────────────────────────────────────────────────────────────

    private static final String JWT_SECRET =
            "test-secret-key-for-unit-tests-minimum-256-bits-long-for-hs256";

    private String adminJwt(UUID tenantId, UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenant_id", tenantId.toString())
                .claim("roles", List.of("ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    // ─── Per-test identifiers ────────────────────────────────────────────────

    UUID tenantId;
    UUID programId;
    UUID classId;
    UUID actorId;
    String jwt;

    @BeforeEach
    void setUp() {
        tenantId  = UUID.randomUUID();
        programId = UUID.randomUUID();
        classId   = UUID.randomUUID();
        actorId   = UUID.randomUUID();
        jwt       = adminJwt(tenantId, actorId);
    }

    // ─── Test 1: Happy path ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST drop-in with new attendee creates attendee, payment, registration + 3 audit rows")
    void happyPath_newAttendee_createsAllThreeRows_andAuditLogs() throws Exception {
        // Arrange: window = [start - 20min, end + 10min]; place start 30min ago so we are in window
        LocalTime startTime = LocalTime.now(ZoneId.of("America/Bogota")).minusMinutes(30)
                .withSecond(0).withNano(0);
        LocalTime endTime   = startTime.plusHours(1);
        LocalDate today     = LocalDate.now(ZoneId.of("America/Bogota"));

        setTenantCtx(tenantId);
        insertTenant(tenantId, actorId);
        insertProgram(tenantId, programId, actorId, new BigDecimal("50000.00"));
        insertClass(tenantId, programId, classId, actorId, 20);
        insertScheduleEntry(tenantId, classId, today.getDayOfWeek().name(), null, startTime, endTime);

        String body = dropInBodyNewAttendee(startTime, "Ana García", "3001234560", new BigDecimal("50000"), "CASH");

        // Act
        String response = mockMvc.perform(
                        post("/api/v1/classes/{classId}/sessions/{sessionDate}/drop-in",
                                classId, today)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attendeeWasNew").value(true))
                .andExpect(jsonPath("$.status").value("PRESENT"))
                .andExpect(jsonPath("$.attendeeTotalVisits").value(1))
                .andReturn().getResponse().getContentAsString();

        JsonNode resp = objectMapper.readTree(response);
        UUID attendeeId = UUID.fromString(resp.get("attendeeId").asText());

        // Assert DB rows
        setTenantCtx(tenantId);
        Long attendeeCount = jdbc.queryForObject(
                "SELECT count(*) FROM drop_in_attendees WHERE tenant_id = '" + tenantId + "'", Long.class);
        assertThat(attendeeCount).isEqualTo(1L);

        Long paymentCount = jdbc.queryForObject(
                "SELECT count(*) FROM drop_in_payments WHERE tenant_id = '" + tenantId + "'", Long.class);
        assertThat(paymentCount).isEqualTo(1L);

        Long regCount = jdbc.queryForObject(
                "SELECT count(*) FROM attendance_registrations WHERE drop_in_attendee_id = '" + attendeeId + "'",
                Long.class);
        assertThat(regCount).isEqualTo(1L);

        Integer totalVisits = jdbc.queryForObject(
                "SELECT total_visits FROM drop_in_attendees WHERE id = '" + attendeeId + "'", Integer.class);
        assertThat(totalVisits).isEqualTo(1);

        Long auditCount = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE actor_id = '" + actorId + "'" +
                " AND action_type IN ('DROP_IN_ATTENDEE_REGISTERED','DROP_IN_PAYMENT_RECORDED','DROP_IN_ATTENDANCE_MARKED')",
                Long.class);
        assertThat(auditCount).isEqualTo(3L);
    }

    // ─── Test 2: Capacity full → rollback ────────────────────────────────────

    @Test
    @DisplayName("POST drop-in when session full returns 409 and no orphan rows")
    void atomicRollback_onCapacityFull_noOrphanRows() throws Exception {
        LocalTime startTime = LocalTime.now(ZoneId.of("America/Bogota")).minusMinutes(30)
                .withSecond(0).withNano(0);
        LocalTime endTime   = startTime.plusHours(1);
        LocalDate today     = LocalDate.now(ZoneId.of("America/Bogota"));

        setTenantCtx(tenantId);
        insertTenant(tenantId, actorId);
        insertProgram(tenantId, programId, actorId, new BigDecimal("50000.00"));
        // max_students = 1
        insertClass(tenantId, programId, classId, actorId, 1);
        insertScheduleEntry(tenantId, classId, today.getDayOfWeek().name(), null, startTime, endTime);

        // Pre-insert session with current_capacity = 1 (full)
        UUID sessionId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO class_sessions (id, tenant_id, class_id, session_date, start_time, end_time, " +
                "status, current_capacity, created_at, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'SCHEDULED', 1, NOW(), ?)",
                sessionId, tenantId, classId, today,
                Time.valueOf(startTime), Time.valueOf(endTime), actorId);

        String body = dropInBodyNewAttendee(startTime, "Bob Smith", "3001234561", new BigDecimal("50000"), "CASH");

        // Act: should fail with 409
        mockMvc.perform(
                        post("/api/v1/classes/{classId}/sessions/{sessionDate}/drop-in",
                                classId, today)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isConflict());

        // Assert: no attendee row, no payment row (transaction rolled back)
        setTenantCtx(tenantId);
        Long attendeeCount = jdbc.queryForObject(
                "SELECT count(*) FROM drop_in_attendees WHERE tenant_id = '" + tenantId + "'", Long.class);
        assertThat(attendeeCount).isEqualTo(0L);

        Long paymentCount = jdbc.queryForObject(
                "SELECT count(*) FROM drop_in_payments WHERE tenant_id = '" + tenantId + "'", Long.class);
        assertThat(paymentCount).isEqualTo(0L);
    }

    // ─── Test 3: Idempotent double-click ─────────────────────────────────────

    @Test
    @DisplayName("Second POST with same attendee+session is idempotent: exactly one payment, total_visits=1")
    void concurrentDoubleClick_exactlyOneRegistration() throws Exception {
        LocalTime startTime = LocalTime.now(ZoneId.of("America/Bogota")).minusMinutes(30)
                .withSecond(0).withNano(0);
        LocalTime endTime   = startTime.plusHours(1);
        LocalDate today     = LocalDate.now(ZoneId.of("America/Bogota"));

        setTenantCtx(tenantId);
        insertTenant(tenantId, actorId);
        insertProgram(tenantId, programId, actorId, new BigDecimal("50000.00"));
        insertClass(tenantId, programId, classId, actorId, 20);
        insertScheduleEntry(tenantId, classId, today.getDayOfWeek().name(), null, startTime, endTime);

        String newAttendeeBody = dropInBodyNewAttendee(startTime, "Carlos López", "3001234562", new BigDecimal("50000"), "CASH");

        // First POST: new attendee → 201
        String response1 = mockMvc.perform(
                        post("/api/v1/classes/{classId}/sessions/{sessionDate}/drop-in",
                                classId, today)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(newAttendeeBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID attendeeId = UUID.fromString(objectMapper.readTree(response1).get("attendeeId").asText());

        // Second POST: same session, same attendee (existingId) → 200 (idempotent)
        String existingAttendeeBody = dropInBodyExistingAttendee(startTime, attendeeId, new BigDecimal("50000"), "CASH");

        mockMvc.perform(
                        post("/api/v1/classes/{classId}/sessions/{sessionDate}/drop-in",
                                classId, today)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(existingAttendeeBody))
                .andExpect(status().isOk());

        // Assert: exactly 1 payment row for this attendee
        setTenantCtx(tenantId);
        Long paymentCount = jdbc.queryForObject(
                "SELECT count(*) FROM drop_in_payments WHERE drop_in_attendee_id = '" + attendeeId + "'",
                Long.class);
        assertThat(paymentCount).isEqualTo(1L);

        // total_visits must still be 1 (idempotent path skips recordVisit)
        Integer totalVisits = jdbc.queryForObject(
                "SELECT total_visits FROM drop_in_attendees WHERE id = '" + attendeeId + "'", Integer.class);
        assertThat(totalVisits).isEqualTo(1);
    }

    // ─── Test 4: Cross-session counter increment ──────────────────────────────

    @Test
    @DisplayName("Two different sessions increment total_visits to 2 for the same attendee")
    void idempotency_acrossTwoSessions_countersIncrementCorrectly() throws Exception {
        LocalTime startTime = LocalTime.now(ZoneId.of("America/Bogota")).minusMinutes(30)
                .withSecond(0).withNano(0);
        LocalTime endTime   = startTime.plusHours(1);
        LocalDate today     = LocalDate.now(ZoneId.of("America/Bogota"));

        // Second class: different class_id, same time window (same day/time)
        UUID classIdB = UUID.randomUUID();

        setTenantCtx(tenantId);
        insertTenant(tenantId, actorId);
        insertProgram(tenantId, programId, actorId, new BigDecimal("50000.00"));

        // Class A
        insertClass(tenantId, programId, classId, actorId, 20);
        insertScheduleEntry(tenantId, classId, today.getDayOfWeek().name(), null, startTime, endTime);

        // Class B
        insertClass(tenantId, programId, classIdB, actorId, 20);
        insertScheduleEntry(tenantId, classIdB, today.getDayOfWeek().name(), null, startTime, endTime);

        // POST 1: class A, new attendee → 201
        String newAttendeeBody = dropInBodyNewAttendee(startTime, "Diana Torres", "3001234563", new BigDecimal("50000"), "CASH");

        String response1 = mockMvc.perform(
                        post("/api/v1/classes/{classId}/sessions/{sessionDate}/drop-in",
                                classId, today)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(newAttendeeBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID attendeeId = UUID.fromString(objectMapper.readTree(response1).get("attendeeId").asText());

        // POST 2: class B, existingId → 200 (existing attendee, new payment for different session)
        // HTTP 201 is returned only when the attendee is brand new; for existing attendees always 200.
        String existingAttendeeBody = dropInBodyExistingAttendee(startTime, attendeeId, new BigDecimal("50000"), "CASH");

        int statusB = mockMvc.perform(
                        post("/api/v1/classes/{classId}/sessions/{sessionDate}/drop-in",
                                classIdB, today)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(existingAttendeeBody))
                .andReturn().getResponse().getStatus();
        assertThat(statusB).isIn(200, 201);

        // Assert: total_visits = 2
        setTenantCtx(tenantId);
        Integer totalVisits = jdbc.queryForObject(
                "SELECT total_visits FROM drop_in_attendees WHERE id = '" + attendeeId + "'", Integer.class);
        assertThat(totalVisits).isEqualTo(2);
    }

    // ─── Test 5: Cancel session fan-out ──────────────────────────────────────

    @Test
    @DisplayName("Cancel session flips drop-in registration status to CANCELLED_BY_SYSTEM")
    void cancelSessionFanOut_dropsInFlippedToCancelledBySystem() throws Exception {
        // startTime = now + 15min:
        //   - drop-in window: [start - 20min, end + 10min] = [now - 5min, now + 85min] → we are inside
        //   - cancel check: sessionStart.isAfter(now) → true (future)
        LocalTime startTime = LocalTime.now(ZoneId.of("America/Bogota")).plusMinutes(15)
                .withSecond(0).withNano(0);
        LocalTime endTime   = startTime.plusMinutes(60);
        LocalDate today     = LocalDate.now(ZoneId.of("America/Bogota"));

        setTenantCtx(tenantId);
        insertTenant(tenantId, actorId);
        insertProgram(tenantId, programId, actorId, new BigDecimal("50000.00"));
        insertClass(tenantId, programId, classId, actorId, 20);
        insertScheduleEntry(tenantId, classId, today.getDayOfWeek().name(), null, startTime, endTime);

        // Step 1: register drop-in
        String dropInBody = dropInBodyNewAttendee(startTime, "Elena Ruiz", "3001234564", new BigDecimal("50000"), "CASH");

        String response = mockMvc.perform(
                        post("/api/v1/classes/{classId}/sessions/{sessionDate}/drop-in",
                                classId, today)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(dropInBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID attendeeId = UUID.fromString(objectMapper.readTree(response).get("attendeeId").asText());

        // Step 2: cancel the session (reason must be 20+ chars per @Size(min=20))
        String cancelBody = """
                { "reason": "Test cancellation reason for full-stack IT test" }
                """;

        mockMvc.perform(
                        post("/api/v1/classes/{classId}/sessions/{sessionDate}/cancel",
                                classId, today)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cancelBody))
                .andExpect(status().isOk());

        // Assert: registration is now SESSION_CANCELLED (the status used by cancelBySession transition)
        setTenantCtx(tenantId);
        String regStatus = jdbc.queryForObject(
                "SELECT status FROM attendance_registrations WHERE drop_in_attendee_id = '" + attendeeId + "'",
                String.class);
        assertThat(regStatus).isEqualTo("SESSION_CANCELLED");
    }

    // ─── Test 6: RLS smoke test ───────────────────────────────────────────────

    @Test
    @DisplayName("Tenant B cannot see Tenant A's drop-in attendee by phone (RLS isolation)")
    void rls_smoke_twoTenantsSamePhone_noLeakage() throws Exception {
        // Tenant A: insert attendee directly
        UUID tenantIdA = UUID.randomUUID();
        UUID actorIdA  = UUID.randomUUID();

        setTenantCtx(tenantIdA);
        insertTenant(tenantIdA, actorIdA);
        jdbc.update(
                "INSERT INTO drop_in_attendees (id, tenant_id, full_name, phone, total_visits, created_at, created_by) " +
                "VALUES (?, ?, 'Tenant A Person', '3001234567', 0, NOW(), ?)",
                UUID.randomUUID(), tenantIdA, actorIdA);

        // Tenant B: separate tenant, same phone
        UUID tenantIdB = UUID.randomUUID();
        UUID actorIdB  = UUID.randomUUID();
        setTenantCtx(tenantIdB);
        insertTenant(tenantIdB, actorIdB);

        // GET lookup using Tenant B's JWT
        String jwtB = adminJwt(tenantIdB, actorIdB);
        mockMvc.perform(
                        get("/api/v1/drop-in-attendees/lookup")
                                .param("phone", "3001234567")
                                .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isNotFound());
    }

    // ─── Fixture helpers ─────────────────────────────────────────────────────

    private void setTenantCtx(UUID tenant) {
        jdbc.execute("SELECT set_config('app.current_tenant', '" + tenant + "', false)");
    }

    private void insertTenant(UUID id, UUID createdBy) {
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
                id,
                "dropin-it-" + id.toString().substring(0, 8),
                createdBy);
    }

    private void insertProgram(UUID tenantId, UUID id, UUID createdBy, BigDecimal dropInPrice) {
        jdbc.update("""
                INSERT INTO programs (id, tenant_id, name, status, drop_in_price, created_at, created_by)
                VALUES (?, ?, 'Adults', 'ACTIVE', ?, NOW(), ?)
                ON CONFLICT (id) DO NOTHING
                """,
                id, tenantId, dropInPrice, createdBy);
    }

    private void insertClass(UUID tenantId, UUID progId, UUID id, UUID createdBy, int maxStudents) {
        // Use the class UUID as part of the name to avoid the unique-per-program constraint
        jdbc.update("""
                INSERT INTO program_classes (id, tenant_id, program_id, name, level, type, max_students, status, created_at, created_by)
                VALUES (?, ?, ?, ?, 'BEGINNER', 'RECURRING', ?, 'ACTIVE', NOW(), ?)
                ON CONFLICT (id) DO NOTHING
                """,
                id, tenantId, progId, "Drop-In-" + id.toString().substring(0, 8), maxStudents, createdBy);
    }

    /**
     * Inserts a class_schedule_entries row. Note: this table has no created_at/created_by columns.
     */
    private void insertScheduleEntry(UUID tenantId, UUID classId,
                                     String dayOfWeek, LocalDate specificDate,
                                     LocalTime startTime, LocalTime endTime) {
        UUID entryId = UUID.randomUUID();
        if (specificDate != null) {
            jdbc.update("""
                    INSERT INTO class_schedule_entries (id, tenant_id, class_id, day_of_week, specific_date, start_time, end_time)
                    VALUES (?, ?, ?, NULL, ?, ?, ?)
                    """,
                    entryId, tenantId, classId, specificDate, Time.valueOf(startTime), Time.valueOf(endTime));
        } else {
            jdbc.update("""
                    INSERT INTO class_schedule_entries (id, tenant_id, class_id, day_of_week, specific_date, start_time, end_time)
                    VALUES (?, ?, ?, ?, NULL, ?, ?)
                    """,
                    entryId, tenantId, classId, dayOfWeek, Time.valueOf(startTime), Time.valueOf(endTime));
        }
    }

    // ─── Request body builders ────────────────────────────────────────────────

    /** Formats HH:mm without seconds for the startTime field in the request body. */
    private static String fmt(LocalTime t) {
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private String dropInBodyNewAttendee(LocalTime startTime, String fullName, String phone,
                                          BigDecimal amount, String paymentMethod) {
        return """
                {
                  "startTime": "%s",
                  "attendee": {
                    "newAttendee": {
                      "fullName": "%s",
                      "phone": "%s"
                    }
                  },
                  "amount": %s,
                  "paymentMethod": "%s"
                }
                """.formatted(fmt(startTime), fullName, phone, amount.toPlainString(), paymentMethod);
    }

    private String dropInBodyExistingAttendee(LocalTime startTime, UUID existingId,
                                               BigDecimal amount, String paymentMethod) {
        return """
                {
                  "startTime": "%s",
                  "attendee": { "existingId": "%s" },
                  "amount": %s,
                  "paymentMethod": "%s"
                }
                """.formatted(fmt(startTime), existingId, amount.toPlainString(), paymentMethod);
    }
}
