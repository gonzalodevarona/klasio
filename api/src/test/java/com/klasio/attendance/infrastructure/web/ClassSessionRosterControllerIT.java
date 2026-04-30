package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.ClassSessionRosterView;
import com.klasio.attendance.application.port.input.ListClassSessionRosterUseCase;
import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
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
import java.time.LocalTime;
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

@WebMvcTest(controllers = ClassSessionRosterController.class)
@Import({GlobalExceptionHandler.class, ClassSessionRosterControllerIT.TestSecurityConfig.class})
class ClassSessionRosterControllerIT {

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
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean ListClassSessionRosterUseCase rosterUseCase;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CLASS_ID  = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();

    private static final LocalDate FROM = LocalDate.of(2026, 4, 27);
    private static final LocalDate TO   = LocalDate.of(2026, 5,  3);

    // ── Key test: empty-registration class returns session entries ────────────

    @Test
    void getRoster_classWithNoRegistrations_returnsScheduledSessions() throws Exception {
        LocalDate sessionDate = LocalDate.of(2026, 4, 27); // Monday
        LocalTime start       = LocalTime.of(18, 0);
        LocalTime end         = LocalTime.of(19, 0);

        ClassSessionRosterView emptySession = new ClassSessionRosterView(
                sessionDate, start, end,
                "SCHEDULED", null, null,
                List.of()
        );

        when(rosterUseCase.execute(any(), eq(CLASS_ID), eq(FROM), eq(TO),
                eq("ADMIN"), any(), any()))
                .thenReturn(List.of(emptySession));

        mockMvc.perform(rosterRequest(FROM, TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sessionDate").value("2026-04-27"))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[0].registrantCount").value(0))
                .andExpect(jsonPath("$[0].registrants").isArray())
                .andExpect(jsonPath("$[0].registrants.length()").value(0));
    }

    @Test
    void getRoster_multipleEmptySessions_allReturned() throws Exception {
        LocalDate mon = LocalDate.of(2026, 4, 27);
        LocalDate wed = LocalDate.of(2026, 4, 29);
        LocalTime start = LocalTime.of(18, 0);
        LocalTime end   = LocalTime.of(19, 0);

        when(rosterUseCase.execute(any(), eq(CLASS_ID), eq(FROM), eq(TO),
                eq("ADMIN"), any(), any()))
                .thenReturn(List.of(
                        new ClassSessionRosterView(mon, start, end, "SCHEDULED", null, null, List.of()),
                        new ClassSessionRosterView(wed, start, end, "SCHEDULED", null, null, List.of())
                ));

        mockMvc.perform(rosterRequest(FROM, TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].registrantCount").value(0))
                .andExpect(jsonPath("$[1].registrantCount").value(0));
    }

    @Test
    void getRoster_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/classes/{classId}/sessions/registrations", CLASS_ID)
                        .param("from", FROM.toString())
                        .param("to", TO.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MockHttpServletRequestBuilder rosterRequest(LocalDate from, LocalDate to) {
        Map<String, Object> details = new HashMap<>();
        details.put("userId",   USER_ID.toString());
        details.put("tenantId", TENANT_ID.toString());
        details.put("role",     "ADMIN");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@test.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(details);

        return get("/api/v1/classes/{classId}/sessions/registrations", CLASS_ID)
                .param("from", from.toString())
                .param("to",   to.toString())
                .accept(MediaType.APPLICATION_JSON)
                .with(authentication(auth));
    }
}
