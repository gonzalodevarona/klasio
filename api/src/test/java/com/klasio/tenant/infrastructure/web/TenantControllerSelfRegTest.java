package com.klasio.tenant.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import com.klasio.tenant.application.port.input.CreateTenantUseCase;
import com.klasio.tenant.application.port.input.DeactivateTenantUseCase;
import com.klasio.tenant.application.port.input.GetTenantDetailUseCase;
import com.klasio.tenant.application.port.input.ListTenantsUseCase;
import com.klasio.tenant.application.port.input.ToggleSelfRegistrationUseCase;
import com.klasio.tenant.domain.port.LogoStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantController.class)
@Import({GlobalExceptionHandler.class, TenantControllerSelfRegTest.TestSecurityConfig.class})
class TenantControllerSelfRegTest {

    @TestConfiguration
    @EnableMethodSecurity
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private CreateTenantUseCase createTenantUseCase;

    @MockitoBean
    private ListTenantsUseCase listTenantsUseCase;

    @MockitoBean
    private GetTenantDetailUseCase getTenantDetailUseCase;

    @MockitoBean
    private DeactivateTenantUseCase deactivateTenantUseCase;

    @MockitoBean
    private LogoStorage logoStorage;

    @MockitoBean
    private ToggleSelfRegistrationUseCase toggleSelfRegistrationUseCase;

    private static final UUID TENANT_ID = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("SUPERADMIN can toggle self-registration and gets 204")
    void toggleSelfRegistration_returns204_andCallsUseCase() throws Exception {
        doNothing().when(toggleSelfRegistrationUseCase).execute(any());

        mockMvc.perform(patch("/api/v1/tenants/{tenantId}/self-registration", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", true))))
                .andExpect(status().isNoContent());

        verify(toggleSelfRegistrationUseCase).execute(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN is forbidden from toggling self-registration")
    void toggleSelfRegistration_forbiddenForAdmin() throws Exception {
        mockMvc.perform(patch("/api/v1/tenants/{tenantId}/self-registration", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isForbidden());
    }
}
