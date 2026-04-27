package com.klasio.auth.infrastructure.web;

import com.klasio.auth.application.port.input.ListUsersByIdsUseCase;
import com.klasio.auth.application.port.input.ListUsersByIdsUseCase.UserSummary;
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

@WebMvcTest(controllers = UsersLookupController.class)
@Import({GlobalExceptionHandler.class, UsersLookupControllerTest.TestSecurityConfig.class})
class UsersLookupControllerTest {

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

    @MockitoBean ListUsersByIdsUseCase listUsersByIdsUseCase;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();

    private static final String URL = "/api/v1/users/by-ids";

    private UsernamePasswordAuthenticationToken authWithRole(String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        Map<String, Object> details = new HashMap<>();
        details.put("userId", USER_ID.toString());
        details.put("tenantId", TENANT_ID.toString());
        details.put("role", role);
        auth.setDetails(details);
        return auth;
    }

    private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req, String role) {
        return req.with(authentication(authWithRole(role)));
    }

    // ------------------------------------------------------------------
    // GET /users/by-ids returns basic profile for each user
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /users/by-ids returns 200 with basic profile for each requested user")
    void getByIds_returnsBasicProfileForEachUser() throws Exception {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        when(listUsersByIdsUseCase.execute(any(), any()))
                .thenReturn(List.of(
                        new UserSummary(userId1, "Carlos López", "PROFESSOR"),
                        new UserSummary(userId2, "Ana García", "ADMIN")
                ));

        mockMvc.perform(withAuth(
                        get(URL)
                                .param("ids", userId1 + "," + userId2),
                        "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(userId1.toString()))
                .andExpect(jsonPath("$[0].fullName").value("Carlos López"))
                .andExpect(jsonPath("$[0].role").value("PROFESSOR"))
                .andExpect(jsonPath("$[1].id").value(userId2.toString()))
                .andExpect(jsonPath("$[1].fullName").value("Ana García"))
                .andExpect(jsonPath("$[1].role").value("ADMIN"));
    }

    // ------------------------------------------------------------------
    // GET /users/by-ids as STUDENT → 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /users/by-ids as STUDENT returns 403")
    void getByIds_returns403_forStudentRole() throws Exception {
        mockMvc.perform(withAuth(
                        get(URL).param("ids", UUID.randomUUID().toString()),
                        "STUDENT"))
                .andExpect(status().isForbidden());
    }
}
