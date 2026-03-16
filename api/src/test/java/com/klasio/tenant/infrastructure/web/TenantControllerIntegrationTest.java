package com.klasio.tenant.infrastructure.web;

import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import com.klasio.shared.infrastructure.exception.SlugAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.TenantAlreadyInactiveException;
import com.klasio.shared.infrastructure.exception.TenantNotFoundException;
import com.klasio.tenant.application.dto.TenantDetail;
import com.klasio.tenant.application.dto.TenantSummary;
import com.klasio.tenant.application.port.input.CreateTenantUseCase;
import com.klasio.tenant.application.port.input.DeactivateTenantUseCase;
import com.klasio.tenant.application.port.input.GetTenantDetailUseCase;
import com.klasio.tenant.application.port.input.ListTenantsUseCase;
import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantSlug;
import com.klasio.tenant.domain.port.LogoStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import javax.sql.DataSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantController.class)
@Import({GlobalExceptionHandler.class, TenantControllerIntegrationTest.TestSecurityConfig.class})
class TenantControllerIntegrationTest {

    @TestConfiguration
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/tenants/**").hasRole("SUPERADMIN")
                            .anyRequest().authenticated()
                    )
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

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

    private static final UUID USER_ID = UUID.randomUUID();

    private static UsernamePasswordAuthenticationToken superadminAuth() {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"))
        );
        Map<String, Object> details = new HashMap<>();
        details.put("userId", USER_ID.toString());
        auth.setDetails(details);
        return auth;
    }

    private static UsernamePasswordAuthenticationToken adminAuth() {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        Map<String, Object> details = new HashMap<>();
        details.put("userId", USER_ID.toString());
        auth.setDetails(details);
        return auth;
    }

    @Test
    @DisplayName("should return 201 when creating tenant with valid data and SUPERADMIN role")
    void createTenant_validData_returns201() throws Exception {
        Tenant tenant = Tenant.create(
                "Liga Bogota",
                "Football",
                TenantSlug.fromName("Liga Bogota"),
                new ContactInfo("contact@liga.com", "+57 300 1234567", "Bogota"),
                USER_ID,
                null
        );
        tenant.clearDomainEvents();

        when(createTenantUseCase.execute(any())).thenReturn(tenant);

        mockMvc.perform(multipart("/api/v1/tenants")
                        .param("name", "Liga Bogota")
                        .param("sportDiscipline", "Football")
                        .param("contactEmail", "contact@liga.com")
                        .param("contactPhone", "+57 300 1234567")
                        .param("contactAddress", "Bogota")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(authentication(superadminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Liga Bogota"))
                .andExpect(jsonPath("$.sportDiscipline").value("Football"))
                .andExpect(jsonPath("$.slug").value("liga-bogota"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("should return 400 when required fields are missing")
    void createTenant_missingFields_returns400() throws Exception {
        when(createTenantUseCase.execute(any()))
                .thenThrow(new IllegalArgumentException("Name must not be blank"));

        mockMvc.perform(multipart("/api/v1/tenants")
                        .param("sportDiscipline", "Football")
                        .param("contactEmail", "contact@liga.com")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(authentication(superadminAuth())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 409 when slug already exists")
    void createTenant_duplicateSlug_returns409() throws Exception {
        when(createTenantUseCase.execute(any()))
                .thenThrow(new SlugAlreadyExistsException(
                        "A tenant with slug 'liga-bogota' already exists",
                        "liga-bogota-2"
                ));

        mockMvc.perform(multipart("/api/v1/tenants")
                        .param("name", "Liga Bogota")
                        .param("sportDiscipline", "Football")
                        .param("contactEmail", "contact@liga.com")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(authentication(superadminAuth())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SLUG_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.error.suggestedSlug").value("liga-bogota-2"));
    }

    @Test
    @DisplayName("should return 403 when user does not have SUPERADMIN role")
    void createTenant_nonSuperadmin_returns403() throws Exception {
        mockMvc.perform(multipart("/api/v1/tenants")
                        .param("name", "Liga Bogota")
                        .param("sportDiscipline", "Football")
                        .param("contactEmail", "contact@liga.com")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(authentication(adminAuth())))
                .andExpect(status().isForbidden());
    }

    // --- Deactivate Tenant endpoint tests ---

    @Test
    @DisplayName("should return 200 when deactivating an active tenant with SUPERADMIN role")
    void deactivateTenant_activeTenant_returns200() throws Exception {
        Tenant tenant = Tenant.create(
                "Liga Bogota",
                "Football",
                TenantSlug.fromName("Liga Bogota"),
                new ContactInfo("contact@liga.com", "+57 300 1234567", "Bogota"),
                USER_ID,
                null
        );
        tenant.clearDomainEvents();
        tenant.deactivate(USER_ID);
        tenant.clearDomainEvents();

        when(deactivateTenantUseCase.execute(any())).thenReturn(tenant);

        mockMvc.perform(post("/api/v1/tenants/liga-bogota/deactivate")
                        .with(authentication(superadminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.deactivatedBy").value(USER_ID.toString()));
    }

    @Test
    @DisplayName("should return 404 when deactivating a non-existent tenant")
    void deactivateTenant_notFound_returns404() throws Exception {
        when(deactivateTenantUseCase.execute(any()))
                .thenThrow(new TenantNotFoundException("Tenant with slug 'non-existent' not found"));

        mockMvc.perform(post("/api/v1/tenants/non-existent/deactivate")
                        .with(authentication(superadminAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TENANT_NOT_FOUND"));
    }

    @Test
    @DisplayName("should return 409 when deactivating an already inactive tenant")
    void deactivateTenant_alreadyInactive_returns409() throws Exception {
        when(deactivateTenantUseCase.execute(any()))
                .thenThrow(new TenantAlreadyInactiveException(
                        "Tenant with slug 'liga-bogota' is already inactive"));

        mockMvc.perform(post("/api/v1/tenants/liga-bogota/deactivate")
                        .with(authentication(superadminAuth())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TENANT_ALREADY_INACTIVE"));
    }

    @Test
    @DisplayName("should return 403 when deactivating without SUPERADMIN role")
    void deactivateTenant_nonSuperadmin_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/liga-bogota/deactivate")
                        .with(authentication(adminAuth())))
                .andExpect(status().isForbidden());
    }

    // --- List Tenants endpoint tests ---

    @Test
    @DisplayName("should return 200 with paginated list of tenants")
    void listTenants_returnsPagedResults() throws Exception {
        TenantSummary summary = new TenantSummary(
                UUID.randomUUID(),
                "liga-bogota",
                "Liga Bogota",
                "Football",
                "ACTIVE",
                Instant.now()
        );
        Page<TenantSummary> page = new PageImpl<>(
                List.of(summary),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(listTenantsUseCase.execute(any(), isNull())).thenReturn(page);

        mockMvc.perform(get("/api/v1/tenants")
                        .with(authentication(superadminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Liga Bogota"))
                .andExpect(jsonPath("$.content[0].slug").value("liga-bogota"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("should return 200 with filtered tenants when status parameter is provided")
    void listTenants_withStatusFilter_returnsFilteredResults() throws Exception {
        TenantSummary summary = new TenantSummary(
                UUID.randomUUID(),
                "liga-bogota",
                "Liga Bogota",
                "Football",
                "ACTIVE",
                Instant.now()
        );
        Page<TenantSummary> page = new PageImpl<>(
                List.of(summary),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(listTenantsUseCase.execute(any(), eq("ACTIVE"))).thenReturn(page);

        mockMvc.perform(get("/api/v1/tenants")
                        .param("status", "ACTIVE")
                        .with(authentication(superadminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("should return 200 with empty page when no tenants exist")
    void listTenants_noTenants_returnsEmptyPage() throws Exception {
        Page<TenantSummary> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                0
        );

        when(listTenantsUseCase.execute(any(), isNull())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/tenants")
                        .with(authentication(superadminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // --- Get Tenant Detail endpoint tests ---

    @Test
    @DisplayName("should return 200 with tenant detail when tenant exists")
    void getTenantDetail_existingSlug_returns200() throws Exception {
        TenantDetail detail = new TenantDetail(
                UUID.randomUUID(),
                "liga-bogota",
                "Liga Bogota",
                "Football",
                "ACTIVE",
                "https://s3.example.com/logo.png",
                "contact@liga.com",
                "+57 300 1234567",
                "Bogota",
                USER_ID,
                Instant.now(),
                null,
                null
        );

        when(getTenantDetailUseCase.execute("liga-bogota")).thenReturn(detail);

        mockMvc.perform(get("/api/v1/tenants/liga-bogota")
                        .with(authentication(superadminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Liga Bogota"))
                .andExpect(jsonPath("$.slug").value("liga-bogota"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.logoUrl").value("https://s3.example.com/logo.png"))
                .andExpect(jsonPath("$.contactEmail").value("contact@liga.com"));
    }

    @Test
    @DisplayName("should return 404 when tenant slug does not exist")
    void getTenantDetail_nonExistentSlug_returns404() throws Exception {
        when(getTenantDetailUseCase.execute("non-existent"))
                .thenThrow(new TenantNotFoundException("Tenant with slug 'non-existent' not found"));

        mockMvc.perform(get("/api/v1/tenants/non-existent")
                        .with(authentication(superadminAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TENANT_NOT_FOUND"));
    }
}
