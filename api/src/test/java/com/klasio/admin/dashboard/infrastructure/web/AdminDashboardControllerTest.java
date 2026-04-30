package com.klasio.admin.dashboard.infrastructure.web;

import com.klasio.admin.dashboard.application.dto.AdminDashboardDto;
import com.klasio.admin.dashboard.application.port.GetAdminDashboardUseCase;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminDashboardController.class)
@Import({GlobalExceptionHandler.class, AdminDashboardControllerTest.TestSecurityConfig.class})
class AdminDashboardControllerTest {

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
            java.sql.Connection conn = org.mockito.Mockito.mock(java.sql.Connection.class);
            org.mockito.Mockito.when(conn.prepareStatement(org.mockito.Mockito.anyString())).thenReturn(stmt);
            javax.sql.DataSource ds = org.mockito.Mockito.mock(javax.sql.DataSource.class);
            org.mockito.Mockito.when(ds.getConnection()).thenReturn(conn);
            return ds;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAdminDashboardUseCase getDashboard;

    private static final UUID TENANT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private Authentication authAs(String role) {
        Map<String, Object> details = new HashMap<>();
        details.put("tenantId", TENANT_ID.toString());
        details.put("userId", UUID.randomUUID().toString());
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        token.setDetails(details);
        return token;
    }

    @Test
    @DisplayName("returns 200 with full dashboard DTO for ADMIN")
    void returns200ForAdmin() throws Exception {
        AdminDashboardDto dto = new AdminDashboardDto(10, 2, 90, 100, 3, 5, List.of());
        when(getDashboard.execute(any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/admin/dashboard").with(authentication(authAs("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentCount").value(10))
                .andExpect(jsonPath("$.newStudentsThisMonth").value(2))
                .andExpect(jsonPath("$.totalHoursConsumed").value(100))
                .andExpect(jsonPath("$.pendingPaymentProofs").value(3))
                .andExpect(jsonPath("$.activeProgramCount").value(5))
                .andExpect(jsonPath("$.students").isArray());
    }

    @Test
    @DisplayName("returns 200 for MANAGER role")
    void returns200ForManager() throws Exception {
        when(getDashboard.execute(any())).thenReturn(new AdminDashboardDto(0, 0, 0, 0, 0, 0, List.of()));

        mockMvc.perform(get("/api/v1/admin/dashboard").with(authentication(authAs("MANAGER"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("returns 200 for SUPERADMIN role")
    void returns200ForSuperadmin() throws Exception {
        when(getDashboard.execute(any())).thenReturn(new AdminDashboardDto(0, 0, 0, 0, 0, 0, List.of()));

        mockMvc.perform(get("/api/v1/admin/dashboard").with(authentication(authAs("SUPERADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("returns 403 for STUDENT role")
    void returns403ForStudent() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard").with(authentication(authAs("STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("returns 403 for PROFESSOR role")
    void returns403ForProfessor() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard").with(authentication(authAs("PROFESSOR"))))
                .andExpect(status().isForbidden());
    }
}
