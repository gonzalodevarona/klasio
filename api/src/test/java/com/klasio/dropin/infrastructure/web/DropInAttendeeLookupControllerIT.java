package com.klasio.dropin.infrastructure.web;

import com.klasio.dropin.application.dto.DropInAttendeeLookupResult;
import com.klasio.dropin.application.service.LookupDropInAttendeeService;
import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.DropInAttendeeNotFoundException;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DropInAttendeeLookupController.class)
@Import({GlobalExceptionHandler.class, DropInAttendeeLookupControllerIT.TestSecurityConfig.class})
class DropInAttendeeLookupControllerIT {

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
    @MockitoBean LookupDropInAttendeeService lookupDropInAttendeeService;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();

    private static final String LOOKUP_URL = "/api/v1/drop-in-attendees/lookup";
    private static final String PHONE      = "3001234567";

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
    // Test 1: GET /lookup with ADMIN role → 200 with lookup result
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /lookup with ADMIN role returns 200 with attendee data")
    void lookup_asAdmin_returns200() throws Exception {
        UUID attendeeId = UUID.randomUUID();
        Instant firstVisit = Instant.parse("2026-01-01T10:00:00Z");
        Instant lastVisit  = Instant.parse("2026-05-01T10:00:00Z");

        when(lookupDropInAttendeeService.lookup(eq(TENANT_ID), eq(PHONE)))
                .thenReturn(new DropInAttendeeLookupResult(
                        attendeeId, "Jane Doe", PHONE, 5,
                        firstVisit, lastVisit, false));

        mockMvc.perform(withAuth(
                        get(LOOKUP_URL)
                                .param("phone", PHONE)
                                .accept(MediaType.APPLICATION_JSON),
                        "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(attendeeId.toString()))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.phone").value(PHONE))
                .andExpect(jsonPath("$.totalVisits").value(5))
                .andExpect(jsonPath("$.converted").value(false));
    }

    // ------------------------------------------------------------------
    // Test 2: GET /lookup with PROFESSOR role → 200
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /lookup with PROFESSOR role returns 200")
    void lookup_asProfessor_returns200() throws Exception {
        UUID attendeeId = UUID.randomUUID();
        Instant now = Instant.now();

        when(lookupDropInAttendeeService.lookup(any(), eq(PHONE)))
                .thenReturn(new DropInAttendeeLookupResult(
                        attendeeId, "John Smith", PHONE, 2,
                        now, now, false));

        mockMvc.perform(withAuth(
                        get(LOOKUP_URL)
                                .param("phone", PHONE)
                                .accept(MediaType.APPLICATION_JSON),
                        "PROFESSOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(attendeeId.toString()));
    }

    // ------------------------------------------------------------------
    // Test 3: GET /lookup when attendee not found → 404
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /lookup for unknown phone returns 404")
    void lookup_notFound_returns404() throws Exception {
        when(lookupDropInAttendeeService.lookup(any(), eq(PHONE)))
                .thenThrow(new DropInAttendeeNotFoundException("No attendee with phone " + PHONE));

        mockMvc.perform(withAuth(
                        get(LOOKUP_URL)
                                .param("phone", PHONE)
                                .accept(MediaType.APPLICATION_JSON),
                        "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DROP_IN_ATTENDEE_NOT_FOUND"));
    }

    // ------------------------------------------------------------------
    // Test 4: GET /lookup with STUDENT role → 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /lookup with STUDENT role returns 403")
    void lookup_studentRole_returns403() throws Exception {
        mockMvc.perform(withAuth(
                        get(LOOKUP_URL)
                                .param("phone", PHONE)
                                .accept(MediaType.APPLICATION_JSON),
                        "STUDENT"))
                .andExpect(status().isForbidden());
    }
}
