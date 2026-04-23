package com.klasio.program.infrastructure.web;

import com.klasio.program.application.dto.ProgramDetail;
import com.klasio.program.application.dto.ProgramSummary;
import com.klasio.program.application.port.input.CreateProgramUseCase;
import com.klasio.program.application.port.input.DeactivateProgramUseCase;
import com.klasio.program.application.port.input.GetProgramDetailUseCase;
import com.klasio.program.application.port.input.ListProgramsUseCase;
import com.klasio.program.application.port.input.ReactivateProgramUseCase;
import com.klasio.program.application.port.input.UpdateProgramUseCase;
import com.klasio.program.domain.model.Program;
import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import com.klasio.shared.infrastructure.exception.ProgramAlreadyActiveException;
import com.klasio.shared.infrastructure.exception.ProgramAlreadyInactiveException;
import com.klasio.shared.infrastructure.exception.ProgramNameAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.ProgramNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProgramController.class)
@Import({GlobalExceptionHandler.class, ProgramControllerIntegrationTest.TestSecurityConfig.class})
class ProgramControllerIntegrationTest {

    @TestConfiguration
    @EnableMethodSecurity
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/programs/**").authenticated()
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
    private CreateProgramUseCase createProgramUseCase;

    @MockitoBean
    private ListProgramsUseCase listProgramsUseCase;

    @MockitoBean
    private GetProgramDetailUseCase getProgramDetailUseCase;

    @MockitoBean
    private UpdateProgramUseCase updateProgramUseCase;

    @MockitoBean
    private DeactivateProgramUseCase deactivateProgramUseCase;

    @MockitoBean
    private ReactivateProgramUseCase reactivateProgramUseCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws SQLException {
        reset(createProgramUseCase, listProgramsUseCase, getProgramDetailUseCase,
                updateProgramUseCase, deactivateProgramUseCase, reactivateProgramUseCase);

        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true);
        when(mockRs.getString("status")).thenReturn("ACTIVE");
    }

    private static UsernamePasswordAuthenticationToken adminAuth() {
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

    private static UsernamePasswordAuthenticationToken managerAuth() {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );
        Map<String, Object> details = new HashMap<>();
        details.put("userId", USER_ID.toString());
        details.put("tenantId", TENANT_ID.toString());
        auth.setDetails(details);
        return auth;
    }

    private static UsernamePasswordAuthenticationToken studentAuth() {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        Map<String, Object> details = new HashMap<>();
        details.put("userId", USER_ID.toString());
        details.put("tenantId", TENANT_ID.toString());
        auth.setDetails(details);
        return auth;
    }

    private Program buildActiveProgram() {
        Program program = Program.create(TENANT_ID, "Kids Soccer", USER_ID);
        program.clearDomainEvents();
        return program;
    }

    @Nested
    @DisplayName("POST /api/v1/programs")
    class CreateProgram {

        @Test
        @DisplayName("should return 201 when admin creates program with valid data")
        void validAdmin_returns201() throws Exception {
            when(createProgramUseCase.execute(any())).thenReturn(buildActiveProgram());

            mockMvc.perform(post("/api/v1/programs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Kids Soccer"
                                    }
                                    """)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Kids Soccer"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should return 400 when required fields are missing")
        void missingFields_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/programs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 409 when program name already exists in tenant")
        void duplicateName_returns409() throws Exception {
            when(createProgramUseCase.execute(any()))
                    .thenThrow(new ProgramNameAlreadyExistsException(
                            "A program with name 'Kids Soccer' already exists in this tenant"));

            mockMvc.perform(post("/api/v1/programs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Kids Soccer"
                                    }
                                    """)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("PROGRAM_NAME_ALREADY_EXISTS"));
        }

        @Test
        @DisplayName("should return 403 when manager tries to create program")
        void managerRole_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/programs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Kids Soccer"
                                    }
                                    """)
                            .with(authentication(managerAuth())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when student tries to create program")
        void studentRole_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/programs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Kids Soccer"
                                    }
                                    """)
                            .with(authentication(studentAuth())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/programs")
    class ListPrograms {

        @Test
        @DisplayName("should return 200 with paginated list for admin")
        void admin_returns200WithPaginatedList() throws Exception {
            var summary = new ProgramSummary(
                    UUID.randomUUID(), "Kids Soccer", "ACTIVE", Instant.now());
            var page = new PageImpl<>(
                    List.of(summary),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1);

            when(listProgramsUseCase.execute(eq(TENANT_ID), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/programs")
                            .with(authentication(adminAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Kids Soccer"))
                    .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 200 with filtered list when status provided")
        void withStatusFilter_returns200() throws Exception {
            var summary = new ProgramSummary(
                    UUID.randomUUID(), "Inactive Program", "INACTIVE", Instant.now());
            var page = new PageImpl<>(
                    List.of(summary),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1);

            when(listProgramsUseCase.execute(eq(TENANT_ID), any(), eq("INACTIVE"))).thenReturn(page);

            mockMvc.perform(get("/api/v1/programs")
                            .param("status", "INACTIVE")
                            .with(authentication(adminAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("INACTIVE"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 200 with empty page when no programs exist")
        void noPrograms_returnsEmptyPage() throws Exception {
            var emptyPage = new PageImpl<ProgramSummary>(
                    Collections.emptyList(),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    0);

            when(listProgramsUseCase.execute(eq(TENANT_ID), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/programs")
                            .with(authentication(adminAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("should return 200 when manager accesses program list")
        void managerRole_returns200() throws Exception {
            var emptyPage = new PageImpl<ProgramSummary>(
                    Collections.emptyList(),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    0);

            when(listProgramsUseCase.execute(eq(TENANT_ID), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/programs")
                            .with(authentication(managerAuth())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/programs/{programId}")
    class GetProgramDetail {

        @Test
        @DisplayName("should return 200 with program detail for admin")
        void admin_returns200() throws Exception {
            UUID programId = UUID.randomUUID();
            var detail = new ProgramDetail(
                    programId, TENANT_ID, "Kids Soccer", "ACTIVE",
                    Instant.now(), USER_ID.toString(), null, null);

            when(getProgramDetailUseCase.execute(TENANT_ID, programId)).thenReturn(detail);

            mockMvc.perform(get("/api/v1/programs/{programId}", programId)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Kids Soccer"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should return 404 when program does not exist")
        void notFound_returns404() throws Exception {
            UUID programId = UUID.randomUUID();
            when(getProgramDetailUseCase.execute(TENANT_ID, programId))
                    .thenThrow(new ProgramNotFoundException("Program not found"));

            mockMvc.perform(get("/api/v1/programs/{programId}", programId)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("PROGRAM_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 200 when manager accesses program detail")
        void managerRole_returns200() throws Exception {
            UUID programId = UUID.randomUUID();
            var detail = new ProgramDetail(
                    programId, TENANT_ID, "Kids Soccer", "ACTIVE",
                    Instant.now(), USER_ID.toString(), null, null);

            when(getProgramDetailUseCase.execute(TENANT_ID, programId)).thenReturn(detail);

            mockMvc.perform(get("/api/v1/programs/{programId}", programId)
                            .with(authentication(managerAuth())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/programs/{programId}")
    class UpdateProgram {

        @Test
        @DisplayName("should return 200 when admin updates program")
        void validAdmin_returns200() throws Exception {
            UUID programId = UUID.randomUUID();
            Program program = buildActiveProgram();
            when(updateProgramUseCase.execute(any())).thenReturn(program);

            mockMvc.perform(put("/api/v1/programs/{programId}", programId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Updated Name"
                                    }
                                    """)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Kids Soccer"));
        }

        @Test
        @DisplayName("should return 409 when updated name already exists")
        void duplicateName_returns409() throws Exception {
            UUID programId = UUID.randomUUID();
            when(updateProgramUseCase.execute(any()))
                    .thenThrow(new ProgramNameAlreadyExistsException("Name already exists"));

            mockMvc.perform(put("/api/v1/programs/{programId}", programId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Existing Name"
                                    }
                                    """)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("PROGRAM_NAME_ALREADY_EXISTS"));
        }

        @Test
        @DisplayName("should return 404 when program does not exist")
        void notFound_returns404() throws Exception {
            UUID programId = UUID.randomUUID();
            when(updateProgramUseCase.execute(any()))
                    .thenThrow(new ProgramNotFoundException("Program not found"));

            mockMvc.perform(put("/api/v1/programs/{programId}", programId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Some Name"
                                    }
                                    """)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("PROGRAM_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 403 when manager tries to update program")
        void managerRole_returns403() throws Exception {
            UUID programId = UUID.randomUUID();
            mockMvc.perform(put("/api/v1/programs/{programId}", programId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Updated Name"
                                    }
                                    """)
                            .with(authentication(managerAuth())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/programs/{programId}/deactivate")
    class DeactivateProgram {

        @Test
        @DisplayName("should return 200 when deactivating active program")
        void activeProgram_returns200() throws Exception {
            UUID programId = UUID.randomUUID();
            Program program = buildActiveProgram();
            program.deactivate(USER_ID);
            program.clearDomainEvents();

            when(deactivateProgramUseCase.execute(TENANT_ID, programId, USER_ID)).thenReturn(program);

            mockMvc.perform(post("/api/v1/programs/{programId}/deactivate", programId)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("INACTIVE"));
        }

        @Test
        @DisplayName("should return 409 when program is already inactive")
        void alreadyInactive_returns409() throws Exception {
            UUID programId = UUID.randomUUID();
            when(deactivateProgramUseCase.execute(TENANT_ID, programId, USER_ID))
                    .thenThrow(new ProgramAlreadyInactiveException("Program is already inactive"));

            mockMvc.perform(post("/api/v1/programs/{programId}/deactivate", programId)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("PROGRAM_ALREADY_INACTIVE"));
        }

        @Test
        @DisplayName("should return 404 when program does not exist")
        void notFound_returns404() throws Exception {
            UUID programId = UUID.randomUUID();
            when(deactivateProgramUseCase.execute(TENANT_ID, programId, USER_ID))
                    .thenThrow(new ProgramNotFoundException("Program not found"));

            mockMvc.perform(post("/api/v1/programs/{programId}/deactivate", programId)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("PROGRAM_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 403 when manager tries to deactivate program")
        void managerRole_returns403() throws Exception {
            UUID programId = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/programs/{programId}/deactivate", programId)
                            .with(authentication(managerAuth())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/programs/{programId}/reactivate")
    class ReactivateProgram {

        @Test
        @DisplayName("should return 200 when reactivating inactive program")
        void inactiveProgram_returns200() throws Exception {
            UUID programId = UUID.randomUUID();
            Program program = buildActiveProgram();
            program.deactivate(USER_ID);
            program.clearDomainEvents();
            program.reactivate(USER_ID);
            program.clearDomainEvents();

            when(reactivateProgramUseCase.execute(TENANT_ID, programId, USER_ID)).thenReturn(program);

            mockMvc.perform(post("/api/v1/programs/{programId}/reactivate", programId)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should return 409 when program is already active")
        void alreadyActive_returns409() throws Exception {
            UUID programId = UUID.randomUUID();
            when(reactivateProgramUseCase.execute(TENANT_ID, programId, USER_ID))
                    .thenThrow(new ProgramAlreadyActiveException("Program is already active"));

            mockMvc.perform(post("/api/v1/programs/{programId}/reactivate", programId)
                            .with(authentication(adminAuth())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("PROGRAM_ALREADY_ACTIVE"));
        }

        @Test
        @DisplayName("should return 403 when manager tries to reactivate program")
        void managerRole_returns403() throws Exception {
            UUID programId = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/programs/{programId}/reactivate", programId)
                            .with(authentication(managerAuth())))
                    .andExpect(status().isForbidden());
        }
    }
}
