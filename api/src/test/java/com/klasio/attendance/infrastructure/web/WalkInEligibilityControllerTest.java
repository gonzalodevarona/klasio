package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.port.input.ListEligibleStudentsUseCase;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort.EligibleStudentView;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WalkInEligibilityController.class)
@Import({GlobalExceptionHandler.class, WalkInEligibilityControllerTest.TestSecurityConfig.class})
class WalkInEligibilityControllerTest {

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

    @MockitoBean ListEligibleStudentsUseCase listEligibleStudentsUseCase;

    private static final UUID TENANT_ID   = UUID.randomUUID();
    private static final UUID CLASS_ID    = UUID.randomUUID();
    private static final UUID USER_ID     = UUID.randomUUID();
    private static final UUID PROGRAM_ID  = UUID.randomUUID();
    private static final LocalDate SESSION_DATE = LocalDate.now().plusDays(1);

    private static final String ELIGIBLE_URL =
            "/api/v1/classes/{classId}/sessions/{sessionDate}/walk-in/eligible-students";

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
    // GET /eligible-students as PROFESSOR → 200 with results
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /eligible-students returns 200 with results")
    void list_returns200_withResults() throws Exception {
        UUID studentId    = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();

        when(listEligibleStudentsUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new EligibleStudentView(
                        studentId, "Ana Ruiz", "1234567", enrollmentId, membershipId, 3, "BEGINNER")));

        mockMvc.perform(withAuth(
                        get(ELIGIBLE_URL, CLASS_ID, SESSION_DATE)
                                .param("startTime", "18:00:00"),
                        "PROFESSOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(studentId.toString()))
                .andExpect(jsonPath("$[0].fullName").value("Ana Ruiz"))
                .andExpect(jsonPath("$[0].idDocument").value("1234567"))
                .andExpect(jsonPath("$[0].availableHours").value(3));
    }

    // ------------------------------------------------------------------
    // GET /eligible-students as STUDENT → 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /eligible-students as STUDENT returns 403")
    void list_returns403_forStudentRole() throws Exception {
        mockMvc.perform(withAuth(
                        get(ELIGIBLE_URL, CLASS_ID, SESSION_DATE)
                                .param("startTime", "18:00:00"),
                        "STUDENT"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // GET /eligible-students passes name filter to use case
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /eligible-students passes q param as name filter to use case")
    void list_passesNameFilterToService() throws Exception {
        when(listEligibleStudentsUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(withAuth(
                        get(ELIGIBLE_URL, CLASS_ID, SESSION_DATE)
                                .param("startTime", "18:00:00")
                                .param("q", "Ana"),
                        "ADMIN"))
                .andExpect(status().isOk());

        verify(listEligibleStudentsUseCase).execute(
                any(), any(), any(), any(), eq("Ana"), any(), any(), any());
    }
}
