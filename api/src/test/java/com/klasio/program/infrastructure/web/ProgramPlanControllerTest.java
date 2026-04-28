package com.klasio.program.infrastructure.web;

import com.klasio.program.application.port.input.CreateProgramPlanUseCase;
import com.klasio.program.application.port.input.DeactivateProgramPlanUseCase;
import com.klasio.program.application.port.input.GetProgramPlanDetailUseCase;
import com.klasio.program.application.port.input.ListProgramPlansUseCase;
import com.klasio.program.application.port.input.ReactivateProgramPlanUseCase;
import com.klasio.program.application.port.input.UpdateProgramPlanUseCase;
import com.klasio.program.domain.model.ProgramModality;
import com.klasio.program.domain.model.ProgramPlan;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer tests for ProgramPlanController.
 * Focus: verify that an UNLIMITED plan request (no hours, no scheduleEntries) is accepted
 * and returns 201 with modality=UNLIMITED and null hours in the response.
 */
@WebMvcTest(controllers = ProgramPlanController.class)
@Import({GlobalExceptionHandler.class, ProgramPlanControllerTest.TestSecurityConfig.class})
class ProgramPlanControllerTest {

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
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getString("status")).thenReturn("ACTIVE");

            PreparedStatement stmt = mock(PreparedStatement.class);
            when(stmt.executeQuery()).thenReturn(rs);
            when(stmt.execute()).thenReturn(false);

            Connection conn = mock(Connection.class);
            when(conn.prepareStatement(any())).thenReturn(stmt);

            DataSource ds = mock(DataSource.class);
            when(ds.getConnection()).thenReturn(conn);
            return ds;
        }

        @Bean
        public org.springframework.security.core.userdetails.UserDetailsService noOpUserDetailsService() {
            return username -> {
                throw new UsernameNotFoundException("Test: no JDBC user store");
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CreateProgramPlanUseCase createProgramPlanUseCase;
    @MockitoBean private ListProgramPlansUseCase listProgramPlansUseCase;
    @MockitoBean private GetProgramPlanDetailUseCase getProgramPlanDetailUseCase;
    @MockitoBean private UpdateProgramPlanUseCase updateProgramPlanUseCase;
    @MockitoBean private DeactivateProgramPlanUseCase deactivateProgramPlanUseCase;
    @MockitoBean private ReactivateProgramPlanUseCase reactivateProgramPlanUseCase;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID MANAGER_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken adminAuth() {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        Map<String, Object> details = new HashMap<>();
        details.put("userId", USER_ID.toString());
        details.put("tenantId", TENANT_ID.toString());
        auth.setDetails(details);
        return auth;
    }

    /**
     * Builds an UNLIMITED ProgramPlan via the domain factory — hours and
     * scheduleEntries are both absent from the request, which is the valid shape for UNLIMITED.
     */
    private ProgramPlan buildUnlimitedPlan() {
        ProgramPlan plan = ProgramPlan.create(
                PROGRAM_ID,
                TENANT_ID,
                "Unlimited Monthly",
                ProgramModality.UNLIMITED,
                new BigDecimal("80.00"),
                null,                    // hours — null for UNLIMITED
                Collections.emptyList(), // scheduleEntries — empty for UNLIMITED
                MANAGER_ID,
                USER_ID
        );
        plan.clearDomainEvents();
        return plan;
    }

    @Test
    @DisplayName("POST /programs/{id}/plans with UNLIMITED modality and no hours returns 201")
    void createProgramPlan_withUnlimitedModality_returns201() throws Exception {
        ProgramPlan unlimitedPlan = buildUnlimitedPlan();
        when(createProgramPlanUseCase.execute(any())).thenReturn(unlimitedPlan);

        // No "hours" field, no "scheduleEntries" field — valid for UNLIMITED
        String requestBody = """
                {
                  "name": "Unlimited Monthly",
                  "modality": "UNLIMITED",
                  "cost": 80.00,
                  "managerId": "%s"
                }
                """.formatted(MANAGER_ID);

        mockMvc.perform(post("/api/v1/programs/{programId}/plans", PROGRAM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modality").value("UNLIMITED"))
                .andExpect(jsonPath("$.hours").doesNotExist())
                .andExpect(jsonPath("$.name").value("Unlimited Monthly"));
    }

    @Test
    @DisplayName("POST /programs/{id}/plans with UNLIMITED modality and explicit null hours returns 201")
    void createProgramPlan_withUnlimitedModalityAndExplicitNullHours_returns201() throws Exception {
        ProgramPlan unlimitedPlan = buildUnlimitedPlan();
        when(createProgramPlanUseCase.execute(any())).thenReturn(unlimitedPlan);

        // hours explicitly set to null in the JSON body — must also be accepted
        String requestBody = """
                {
                  "name": "Unlimited Monthly",
                  "modality": "UNLIMITED",
                  "cost": 80.00,
                  "hours": null,
                  "managerId": "%s"
                }
                """.formatted(MANAGER_ID);

        mockMvc.perform(post("/api/v1/programs/{programId}/plans", PROGRAM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modality").value("UNLIMITED"))
                .andExpect(jsonPath("$.hours").doesNotExist());
    }
}
