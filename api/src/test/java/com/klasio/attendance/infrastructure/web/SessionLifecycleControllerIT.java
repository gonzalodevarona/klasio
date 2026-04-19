package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.dto.SessionCancellationResult;
import com.klasio.attendance.application.dto.UpdateSessionAlertCommand;
import com.klasio.attendance.application.port.input.CancelSessionUseCase;
import com.klasio.attendance.application.port.input.RaiseSessionAlertUseCase;
import com.klasio.attendance.application.port.input.UpdateSessionAlertUseCase;
import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import com.klasio.shared.infrastructure.exception.NotAlertAuthorException;
import com.klasio.shared.infrastructure.exception.SessionAlreadyCancelledException;
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

import javax.sql.DataSource;
import java.time.Instant;
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

@WebMvcTest(controllers = SessionLifecycleController.class)
@Import({GlobalExceptionHandler.class, SessionLifecycleControllerIT.TestSecurityConfig.class})
class SessionLifecycleControllerIT {

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

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RaiseSessionAlertUseCase raiseSessionAlertUseCase;

    @MockitoBean
    UpdateSessionAlertUseCase updateSessionAlertUseCase;

    @MockitoBean
    CancelSessionUseCase cancelSessionUseCase;

    // ------------------------------------------------------------------
    // Fixed test identifiers
    // ------------------------------------------------------------------

    private static final UUID TENANT_ID     = UUID.randomUUID();
    private static final UUID CLASS_ID      = UUID.randomUUID();
    private static final UUID SESSION_ID    = UUID.randomUUID();
    private static final UUID PROFESSOR_ID  = UUID.randomUUID();
    private static final UUID MANAGER_ID    = UUID.randomUUID();
    private static final UUID PROGRAM_ID    = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    // Future date so timing guards pass
    private static final LocalDate SESSION_DATE = LocalDate.now().plusDays(1);

    private static final String ALERT_URL  = "/api/v1/classes/{classId}/sessions/{sessionDate}/alert";
    private static final String CANCEL_URL = "/api/v1/classes/{classId}/sessions/{sessionDate}/cancel";

    private static final String VALID_REASON = "Professor is running late due to traffic conditions today";
    private static final String SHORT_REASON = "Too short";

    // ------------------------------------------------------------------
    // Auth helpers
    // ------------------------------------------------------------------

    private UsernamePasswordAuthenticationToken authAs(UUID userId, String role) {
        var token = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        Map<String, Object> details = new HashMap<>();
        details.put("userId", userId.toString());
        details.put("tenantId", TENANT_ID.toString());
        details.put("programId", PROGRAM_ID.toString());
        details.put("role", role);
        token.setDetails(details);
        return token;
    }

    private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req, UUID userId, String role) {
        return req.with(authentication(authAs(userId, role)));
    }

    private String reasonBody(String reason) {
        return """
                { "reason": "%s" }
                """.formatted(reason);
    }

    // ------------------------------------------------------------------
    // Scenario 1: POST /alert as PROFESSOR → 201, status=ALERTED
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /alert as PROFESSOR returns 201 with status ALERTED")
    void postAlert_asProfessor_returns201() throws Exception {
        when(raiseSessionAlertUseCase.execute(any())).thenReturn(
                new SessionActionResult(SESSION_ID, "ALERTED", VALID_REASON, PROFESSOR_ID, Instant.now()));

        mockMvc.perform(withAuth(
                        post(ALERT_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reasonBody(VALID_REASON)),
                        PROFESSOR_ID, "PROFESSOR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.status").value("ALERTED"));
    }

    // ------------------------------------------------------------------
    // Scenario 2: POST /alert with reason < 20 chars → 400 (validation)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /alert with short reason returns 400")
    void postAlert_shortReason_returns400() throws Exception {
        mockMvc.perform(withAuth(
                        post(ALERT_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reasonBody(SHORT_REASON)),
                        PROFESSOR_ID, "PROFESSOR"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Scenario 3: POST /alert as a different professor (not assigned) → 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /alert as unrelated user with no PROFESSOR/MANAGER/ADMIN role returns 403")
    void postAlert_asStudent_returns403() throws Exception {
        // Use STUDENT role — not in the allowed set
        var token = new UsernamePasswordAuthenticationToken(
                OTHER_USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        Map<String, Object> details = new HashMap<>();
        details.put("userId", OTHER_USER_ID.toString());
        details.put("tenantId", TENANT_ID.toString());
        details.put("role", "STUDENT");
        token.setDetails(details);

        mockMvc.perform(post(ALERT_URL, CLASS_ID, SESSION_DATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reasonBody(VALID_REASON))
                        .with(authentication(token)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Scenario 4: PATCH /alert by original professor → 200, reason updated
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /alert by professor returns 200 with updated reason")
    void patchAlert_byProfessor_returns200() throws Exception {
        String updatedReason = "Professor is running late due to a heavy rainstorm near campus";

        when(updateSessionAlertUseCase.execute(any(UpdateSessionAlertCommand.class))).thenReturn(
                new SessionActionResult(SESSION_ID, "ALERTED", updatedReason, PROFESSOR_ID, Instant.now()));

        mockMvc.perform(withAuth(
                        patch(ALERT_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reasonBody(updatedReason)),
                        PROFESSOR_ID, "PROFESSOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALERTED"))
                .andExpect(jsonPath("$.reason").value(updatedReason));
    }

    // ------------------------------------------------------------------
    // Scenario 5: PATCH /alert by a different user → 403 (NotAlertAuthorException)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /alert by different user returns 403 (not the alert author)")
    void patchAlert_byOtherUser_returns403() throws Exception {
        when(updateSessionAlertUseCase.execute(any(UpdateSessionAlertCommand.class)))
                .thenThrow(new NotAlertAuthorException());

        mockMvc.perform(withAuth(
                        patch(ALERT_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reasonBody(VALID_REASON)),
                        OTHER_USER_ID, "PROFESSOR"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Scenario 6: POST /cancel as MANAGER → 200, status=CANCELLED, affectedStudentCount matches
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /cancel as MANAGER returns 200 with CANCELLED status and affectedStudentCount")
    void postCancel_asManager_returns200() throws Exception {
        int seededRegistrations = 3;

        when(cancelSessionUseCase.execute(any())).thenReturn(
                new SessionCancellationResult(SESSION_ID, "CANCELLED", VALID_REASON,
                        MANAGER_ID, Instant.now(), seededRegistrations));

        mockMvc.perform(withAuth(
                        post(CANCEL_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reasonBody(VALID_REASON)),
                        MANAGER_ID, "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.affectedStudentCount").value(seededRegistrations));
    }

    // ------------------------------------------------------------------
    // Scenario 7: Re-POST /cancel → 409 (session already cancelled)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /cancel on already-cancelled session returns 409")
    void postCancel_alreadyCancelled_returns409() throws Exception {
        when(cancelSessionUseCase.execute(any()))
                .thenThrow(new SessionAlreadyCancelledException());

        mockMvc.perform(withAuth(
                        post(CANCEL_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reasonBody(VALID_REASON)),
                        MANAGER_ID, "MANAGER"))
                .andExpect(status().isConflict());
    }
}
