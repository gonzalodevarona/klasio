package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.MarkAttendanceResult;
import com.klasio.attendance.application.dto.MarkAttendanceResult.MarkedRegistration;
import com.klasio.attendance.application.port.input.CorrectMarkUseCase;
import com.klasio.attendance.application.port.input.MarkAttendanceUseCase;
import com.klasio.attendance.application.port.input.RegisterWalkInUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationId;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.AlreadyMarkedException;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import com.klasio.shared.infrastructure.exception.MarkingWindowException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AttendanceMarkingController.class)
@Import({GlobalExceptionHandler.class, AttendanceMarkingControllerTest.TestSecurityConfig.class})
class AttendanceMarkingControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .exceptionHandling(e -> e.authenticationEntryPoint(
                            (request, response, authException) ->
                                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                    .build();
        }

        @Bean
        public DataSource dataSource() throws Exception {
            java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
            org.mockito.Mockito.when(rs.next()).thenReturn(true);
            org.mockito.Mockito.when(rs.getString("status")).thenReturn("ACTIVE");
            java.sql.PreparedStatement stmt = org.mockito.Mockito.mock(java.sql.PreparedStatement.class);
            org.mockito.Mockito.when(stmt.executeQuery()).thenReturn(rs);
            org.mockito.Mockito.when(stmt.execute()).thenReturn(false);
            java.sql.Connection conn = org.mockito.Mockito.mock(java.sql.Connection.class);
            org.mockito.Mockito.when(conn.prepareStatement(org.mockito.Mockito.anyString())).thenReturn(stmt);
            DataSource ds = org.mockito.Mockito.mock(DataSource.class);
            org.mockito.Mockito.when(ds.getConnection()).thenReturn(conn);
            return ds;
        }

        @Bean
        public org.springframework.security.core.userdetails.UserDetailsService noOpUserDetailsService() {
            return username -> {
                throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Test: no JDBC user store");
            };
        }
    }

    @Autowired MockMvc mockMvc;

    @MockitoBean MarkAttendanceUseCase markAttendanceUseCase;
    @MockitoBean CorrectMarkUseCase correctMarkUseCase;
    @MockitoBean RegisterWalkInUseCase registerWalkInUseCase;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID CLASS_ID   = UUID.randomUUID();
    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID REG_ID     = UUID.randomUUID();
    private static final LocalDate SESSION_DATE = LocalDate.now().plusDays(1);

    private static final String MARKS_URL    = "/api/v1/classes/{classId}/sessions/{sessionDate}/marks";
    private static final String CORRECT_URL  = "/api/v1/classes/{classId}/sessions/{sessionDate}/marks/{regId}/correct";
    private static final String WALK_IN_URL  = "/api/v1/classes/{classId}/sessions/{sessionDate}/walk-in";

    private UsernamePasswordAuthenticationToken authWithRole(String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        Map<String, Object> details = new HashMap<>();
        details.put("userId", USER_ID.toString());
        details.put("tenantId", TENANT_ID.toString());
        details.put("programId", PROGRAM_ID.toString());
        details.put("role", role);
        auth.setDetails(details);
        return auth;
    }

    private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req, String role) {
        return req.with(authentication(authWithRole(role)));
    }

    private static final String MARKS_BODY = """
            {
              "startTime": "09:00:00",
              "marks": [
                { "registrationId": "%s", "mark": "PRESENT" }
              ]
            }
            """.formatted(REG_ID);

    private static final String CORRECT_BODY = """
            {
              "newMark": "ABSENT",
              "reason": "Student was not actually present"
            }
            """;

    // ------------------------------------------------------------------
    // POST /marks as PROFESSOR → 200
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /marks as PROFESSOR returns 200")
    void postMarks_asProfessor_returns200() throws Exception {
        when(markAttendanceUseCase.execute(any()))
                .thenReturn(new MarkAttendanceResult(
                        List.of(new MarkedRegistration(REG_ID, "PRESENT", false))));

        mockMvc.perform(withAuth(
                        post(MARKS_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(MARKS_BODY),
                        "PROFESSOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("PRESENT"))
                .andExpect(jsonPath("$.results[0].noHoursWarning").value(false));
    }

    // ------------------------------------------------------------------
    // POST /marks as ADMIN → 200
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /marks as ADMIN returns 200")
    void postMarks_asAdmin_returns200() throws Exception {
        when(markAttendanceUseCase.execute(any()))
                .thenReturn(new MarkAttendanceResult(
                        List.of(new MarkedRegistration(REG_ID, "ABSENT", false))));

        mockMvc.perform(withAuth(
                        post(MARKS_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(MARKS_BODY),
                        "ADMIN"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // PATCH correct as MANAGER → 200
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /marks/{regId}/correct as MANAGER returns 200")
    void patchCorrect_asManager_returns200() throws Exception {
        when(correctMarkUseCase.execute(any()))
                .thenReturn(new MarkedRegistration(REG_ID, "ABSENT", false));

        mockMvc.perform(withAuth(
                        patch(CORRECT_URL, CLASS_ID, SESSION_DATE, REG_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CORRECT_BODY),
                        "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABSENT"));
    }

    // ------------------------------------------------------------------
    // PATCH correct as PROFESSOR → 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /marks/{regId}/correct as PROFESSOR returns 403")
    void patchCorrect_asProfessor_returns403() throws Exception {
        mockMvc.perform(withAuth(
                        patch(CORRECT_URL, CLASS_ID, SESSION_DATE, REG_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CORRECT_BODY),
                        "PROFESSOR"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // POST with MarkingWindowException → 409
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /marks with MarkingWindowException returns 409")
    void postMarks_markingWindowViolation_returns409() throws Exception {
        when(markAttendanceUseCase.execute(any()))
                .thenThrow(new MarkingWindowException("Outside marking window"));

        mockMvc.perform(withAuth(
                        post(MARKS_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(MARKS_BODY),
                        "PROFESSOR"))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------
    // POST without auth → 401
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /marks without auth returns 401")
    void postMarks_noAuth_returns401() throws Exception {
        mockMvc.perform(post(MARKS_URL, CLASS_ID, SESSION_DATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MARKS_BODY))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Walk-in tests
    // ------------------------------------------------------------------

    private static final String WALK_IN_BODY = """
            {
              "startTime": "18:00:00",
              "studentId": "%s",
              "hoursToCharge": 1
            }
            """.formatted(UUID.randomUUID());

    private AttendanceRegistration buildWalkInRegistration(UUID regId) {
        return AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(regId),
                TENANT_ID,
                UUID.randomUUID(),
                CLASS_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BEGINNER",
                1,
                AttendanceRegistrationStatus.PRESENT,
                SESSION_DATE,
                java.time.LocalTime.of(18, 0),
                java.time.LocalTime.of(19, 0),
                null, null, null,
                java.time.Instant.now(), USER_ID,
                null, null, null,
                java.time.Instant.now(), USER_ID,
                null, null
        );
    }

    @Test
    @DisplayName("POST /walk-in as PROFESSOR returns 201")
    void walkIn_returns201_onSuccess() throws Exception {
        UUID regId = UUID.randomUUID();
        when(registerWalkInUseCase.execute(any())).thenReturn(buildWalkInRegistration(regId));

        mockMvc.perform(withAuth(
                        post(WALK_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(WALK_IN_BODY),
                        "PROFESSOR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationId").value(regId.toString()))
                .andExpect(jsonPath("$.status").value("PRESENT"))
                .andExpect(jsonPath("$.intendedHours").value(1));
    }

    @Test
    @DisplayName("POST /walk-in as STUDENT returns 403")
    void walkIn_returns403_forStudentRole() throws Exception {
        mockMvc.perform(withAuth(
                        post(WALK_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(WALK_IN_BODY),
                        "STUDENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /walk-in with hoursToCharge=-1 returns 400")
    void walkIn_returns400_onValidationFailure_negativeHours() throws Exception {
        String badBody = """
                {
                  "startTime": "18:00:00",
                  "studentId": "%s",
                  "hoursToCharge": -1
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(withAuth(
                        post(WALK_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(badBody),
                        "PROFESSOR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /walk-in with AlreadyMarkedException returns 409")
    void walkIn_returns409_onAlreadyMarked() throws Exception {
        when(registerWalkInUseCase.execute(any()))
                .thenThrow(new AlreadyMarkedException("Already marked"));

        mockMvc.perform(withAuth(
                        post(WALK_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(WALK_IN_BODY),
                        "PROFESSOR"))
                .andExpect(status().isConflict());
    }
}
