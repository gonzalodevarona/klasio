package com.klasio.tenant.infrastructure.web;

import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantId;
import com.klasio.tenant.domain.model.TenantSlug;
import com.klasio.tenant.domain.model.TenantStatus;
import com.klasio.tenant.domain.port.LogoStorage;
import com.klasio.tenant.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MeTenantController.class)
@Import({GlobalExceptionHandler.class, MeTenantControllerIntegrationTest.TestSecurityConfig.class})
class MeTenantControllerIntegrationTest {

    @TestConfiguration
    @EnableConfigurationProperties(JwtProperties.class)
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/me/tenant").authenticated()
                            .anyRequest().authenticated()
                    )
                    .build();
        }
    }

    @Autowired private MockMvc mockMvc;
    @MockitoBean private DataSource dataSource;
    @MockitoBean private TenantRepository tenantRepository;
    @MockitoBean private LogoStorage logoStorage;

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID   = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final ContactInfo CONTACT = new ContactInfo(
            "contact@acme.test", "3000000000", "57",
            "Calle 1", "Bogota", "Cundinamarca", "Colombia"
    );

    /**
     * Stub the DataSource mock so that TenantStatusFilter can complete its
     * tenant-active check without a real DB. Returns "ACTIVE" for every query.
     */
    @BeforeEach
    void stubDataSource() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getString("status")).thenReturn("ACTIVE");

        PreparedStatement stmt = mock(PreparedStatement.class);
        when(stmt.executeQuery()).thenReturn(rs);

        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenReturn(stmt);

        when(dataSource.getConnection()).thenReturn(connection);
    }

    private UsernamePasswordAuthenticationToken auth(String role, UUID tenantId) {
        Map<String, Object> details = new HashMap<>();
        details.put("userId", USER_ID.toString());
        details.put("tenantId", tenantId == null ? null : tenantId.toString());
        details.put("role", role);
        var token = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        token.setDetails(details);
        return token;
    }

    private Tenant tenantWithLogoKey(String logoKey) {
        return Tenant.reconstitute(
                TenantId.of(TENANT_ID),
                new TenantSlug("acme-league"),
                "Acme League",
                "BJJ",
                "en",
                "America/Bogota",
                logoKey,
                CONTACT,
                TenantStatus.ACTIVE,
                Instant.now(),
                USER_ID,
                null, null
        );
    }

    @Test
    @DisplayName("ADMIN with tenant logo: response includes presigned logoUrl")
    void admin_with_logo_returns_presigned_url() throws Exception {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithLogoKey("logos/acme.png")));
        when(logoStorage.generatePresignedUrl("logos/acme.png"))
                .thenReturn("https://s3.example.com/logos/acme.png?signed");

        mockMvc.perform(get("/api/v1/me/tenant").with(authentication(auth("ADMIN", TENANT_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Acme League"))
                .andExpect(jsonPath("$.logoUrl").value("https://s3.example.com/logos/acme.png?signed"));

        verify(logoStorage).generatePresignedUrl(eq("logos/acme.png"));
    }

    @Test
    @DisplayName("MANAGER with tenant having no logo: logoUrl absent and storage not called")
    void manager_without_logo_returns_null_url() throws Exception {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithLogoKey(null)));

        mockMvc.perform(get("/api/v1/me/tenant").with(authentication(auth("MANAGER", TENANT_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme League"))
                .andExpect(jsonPath("$.logoUrl").doesNotExist());

        verifyNoInteractions(logoStorage);
    }

    @Test
    @DisplayName("SUPERADMIN: forbidden by RBAC")
    void superadmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/me/tenant").with(authentication(auth("SUPERADMIN", null))))
                .andExpect(status().isForbidden());
    }
}
