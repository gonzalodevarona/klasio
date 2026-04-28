package com.klasio.programclass.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the OPEN class level in ClassController (RF-36).
 *
 * <p>Covers:
 * <ul>
 *   <li>POST /api/v1/programs/{programId}/classes with level=OPEN → 201, level=OPEN in response</li>
 *   <li>GET /api/v1/programs/{programId}/classes/{classId} → level=OPEN in the detail response</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProgramClassControllerIT {

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Fixed identifiers ───────────────────────────────────────────────────

    private static final UUID TENANT_ID   = UUID.randomUUID();
    private static final UUID PROGRAM_ID  = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();

    /** ID returned by POST — captured after the create call. */
    private UUID createdClassId;

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
        jdbc.execute("SELECT set_config('app.current_tenant', '" + TENANT_ID + "', false)");
        insertTenant();
        insertProgram();
    }

    @AfterEach
    void cleanUp() {
        jdbc.execute("SELECT set_config('app.current_tenant', '" + TENANT_ID + "', false)");
        if (createdClassId != null) {
            jdbc.execute("DELETE FROM class_schedule_entries WHERE class_id = '" + createdClassId + "'");
            jdbc.execute("DELETE FROM program_classes WHERE id = '" + createdClassId + "'");
        }
        jdbc.execute("DELETE FROM programs WHERE tenant_id = '" + TENANT_ID + "'");
        jdbc.execute("DELETE FROM tenants WHERE id = '" + TENANT_ID + "'");
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /programs/{programId}/classes with level=OPEN returns 201 and level=OPEN in body")
    void createOpenLevelClass_returns201AndOpenLevel() throws Exception {
        String requestBody = """
                {
                  "name": "Open Level Class",
                  "level": "OPEN",
                  "type": "RECURRING",
                  "scheduleEntries": [
                    { "dayOfWeek": "WEDNESDAY", "startTime": "10:00", "endTime": "12:00" }
                  ],
                  "maxStudents": 25
                }
                """;

        String response = mockMvc.perform(
                        post("/api/v1/programs/{programId}/classes", PROGRAM_ID)
                                .header("Authorization", "Bearer " + adminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.level").value("OPEN"))
                .andExpect(jsonPath("$.name").value("Open Level Class"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the created class ID for cleanup and the GET assertion
        JsonNode responseJson = objectMapper.readTree(response);
        createdClassId = UUID.fromString(responseJson.get("id").asText());
    }

    @Test
    @DisplayName("GET /programs/{programId}/classes/{classId} returns level=OPEN for a previously-created OPEN class")
    void getOpenLevelClass_returnsOpenLevelInDetail() throws Exception {
        // ── 1. Create the OPEN class ──────────────────────────────────────────
        String createBody = """
                {
                  "name": "Open Detail Class",
                  "level": "OPEN",
                  "type": "RECURRING",
                  "scheduleEntries": [
                    { "dayOfWeek": "FRIDAY", "startTime": "08:00", "endTime": "10:00" }
                  ],
                  "maxStudents": 15
                }
                """;

        String createResponse = mockMvc.perform(
                        post("/api/v1/programs/{programId}/classes", PROGRAM_ID)
                                .header("Authorization", "Bearer " + adminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        createdClassId = UUID.fromString(
                com.jayway.jsonpath.JsonPath.read(createResponse, "$.id").toString());

        // ── 2. Retrieve it and assert level=OPEN ──────────────────────────────
        mockMvc.perform(
                        get("/api/v1/programs/{programId}/classes/{classId}", PROGRAM_ID, createdClassId)
                                .header("Authorization", "Bearer " + adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdClassId.toString()))
                .andExpect(jsonPath("$.level").value("OPEN"));
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
}
