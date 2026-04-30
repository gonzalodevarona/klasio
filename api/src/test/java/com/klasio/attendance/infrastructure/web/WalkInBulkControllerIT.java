package com.klasio.attendance.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klasio.attendance.application.dto.WalkInBulkResult;
import com.klasio.attendance.application.port.input.RegisterWalkInBulkUseCase;
import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WalkInBulkController.class)
@Import({GlobalExceptionHandler.class, WalkInBulkControllerIT.TestSecurityConfig.class})
class WalkInBulkControllerIT {

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

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean RegisterWalkInBulkUseCase registerWalkInBulkUseCase;

    private static final UUID TENANT_ID   = UUID.randomUUID();
    private static final UUID CLASS_ID    = UUID.randomUUID();
    private static final UUID USER_ID     = UUID.randomUUID();
    private static final UUID PROGRAM_ID  = UUID.randomUUID();
    private static final UUID STUDENT_ID  = UUID.randomUUID();
    private static final LocalDate SESSION_DATE = LocalDate.now().plusDays(1);

    private static final String BULK_URL =
            "/api/v1/classes/{classId}/sessions/{sessionDate}/walk-in/bulk";

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

    // ------------------------------------------------------------------
    // POST /bulk as ADMIN → 200 with summary.succeeded in response
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /bulk as ADMIN returns 200 with results")
    void postBulk_admin_returns200WithResults() throws Exception {
        UUID registrationId = UUID.randomUUID();
        WalkInBulkResult result = new WalkInBulkResult(
                List.of(WalkInBulkResult.ResultRow.success(STUDENT_ID, registrationId, "PRESENT", 1)),
                new WalkInBulkResult.Summary(1, 1, 0)
        );
        when(registerWalkInBulkUseCase.execute(any())).thenReturn(result);

        var body = Map.of(
                "startTime", "18:00:00",
                "studentIds", List.of(STUDENT_ID.toString()),
                "hoursToCharge", 1
        );

        mockMvc.perform(withAuth(
                        post(BULK_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.succeeded").value(1))
                .andExpect(jsonPath("$.summary.total").value(1))
                .andExpect(jsonPath("$.summary.failed").value(0))
                .andExpect(jsonPath("$.results[0].outcome").value("SUCCESS"));
    }

    // ------------------------------------------------------------------
    // POST /bulk unauthenticated → 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /bulk without auth returns 403")
    void postBulk_unauthenticated_returns403() throws Exception {
        var body = Map.of(
                "startTime", "18:00:00",
                "studentIds", List.of(STUDENT_ID.toString()),
                "hoursToCharge", 1
        );

        mockMvc.perform(
                        post(BULK_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // POST /bulk with empty studentIds → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /bulk with empty studentIds returns 400")
    void postBulk_emptyStudentIds_returns400() throws Exception {
        var body = Map.of(
                "startTime", "18:00:00",
                "studentIds", List.of(),
                "hoursToCharge", 1
        );

        mockMvc.perform(withAuth(
                        post(BULK_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "ADMIN"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // POST /bulk with invalid startTime → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /bulk with invalid startTime returns 400")
    void postBulk_invalidStartTime_returns400() throws Exception {
        var body = new HashMap<String, Object>();
        body.put("startTime", "99:99:99");
        body.put("studentIds", List.of(UUID.randomUUID().toString()));
        body.put("hoursToCharge", 1);

        mockMvc.perform(withAuth(
                        post(BULK_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "ADMIN"))
                .andExpect(status().isBadRequest());
    }
}
