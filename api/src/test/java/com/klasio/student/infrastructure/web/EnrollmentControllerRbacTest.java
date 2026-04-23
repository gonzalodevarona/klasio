package com.klasio.student.infrastructure.web;

import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import com.klasio.student.application.dto.EnrollmentDetail;
import com.klasio.student.application.port.input.EnrollStudentUseCase;
import com.klasio.student.application.port.input.GetLevelHistoryUseCase;
import com.klasio.student.application.port.input.ListEnrollmentsUseCase;
import com.klasio.student.application.port.input.PromoteStudentUseCase;
import com.klasio.student.application.port.input.UnenrollStudentUseCase;
import com.klasio.student.domain.model.Level;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import com.klasio.membership.domain.port.StudentIdPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EnrollmentController.class)
@Import({GlobalExceptionHandler.class, EnrollmentControllerRbacTest.TestSecurityConfig.class})
class EnrollmentControllerRbacTest {

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
                            .requestMatchers("/api/v1/enrollments/**").authenticated()
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
    private EnrollStudentUseCase enrollStudentUseCase;

    @MockitoBean
    private UnenrollStudentUseCase unenrollStudentUseCase;

    @MockitoBean
    private PromoteStudentUseCase promoteStudentUseCase;

    @MockitoBean
    private ListEnrollmentsUseCase listEnrollmentsUseCase;

    @MockitoBean
    private GetLevelHistoryUseCase getLevelHistoryUseCase;

    @MockitoBean
    private StudentEnrollmentRepository enrollmentRepository;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private StudentIdPort studentIdPort;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true);
        when(mockRs.getString("status")).thenReturn("ACTIVE");
    }

    private static UsernamePasswordAuthenticationToken authWithRole(String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        Map<String, Object> details = new HashMap<>();
        details.put("userId", USER_ID.toString());
        details.put("tenantId", TENANT_ID.toString());
        auth.setDetails(details);
        return auth;
    }

    private StudentEnrollment buildEnrollment() {
        return StudentEnrollment.create(TENANT_ID, STUDENT_ID, PROGRAM_ID, Level.BEGINNER, USER_ID);
    }

    private static final String PROMOTE_REQUEST_JSON = """
            { "level": "INTERMEDIATE" }
            """;

    @Nested
    @DisplayName("POST /api/v1/enrollments/{id}/promote — Promote Student RBAC")
    class PromoteStudentRbac {

        @Test
        @DisplayName("ADMIN can promote student")
        void admin_canPromote() throws Exception {
            when(promoteStudentUseCase.execute(any())).thenReturn(buildEnrollment());

            mockMvc.perform(post("/api/v1/enrollments/{id}/promote", ENROLLMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROMOTE_REQUEST_JSON)
                            .with(authentication(authWithRole("ADMIN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("SUPERADMIN can promote student")
        void superadmin_canPromote() throws Exception {
            when(promoteStudentUseCase.execute(any())).thenReturn(buildEnrollment());

            mockMvc.perform(post("/api/v1/enrollments/{id}/promote", ENROLLMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROMOTE_REQUEST_JSON)
                            .with(authentication(authWithRole("SUPERADMIN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("MANAGER can promote student")
        void manager_canPromote() throws Exception {
            when(promoteStudentUseCase.execute(any())).thenReturn(buildEnrollment());

            mockMvc.perform(post("/api/v1/enrollments/{id}/promote", ENROLLMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROMOTE_REQUEST_JSON)
                            .with(authentication(authWithRole("MANAGER"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PROFESSOR can promote student")
        void professor_canPromote() throws Exception {
            when(promoteStudentUseCase.execute(any())).thenReturn(buildEnrollment());

            mockMvc.perform(post("/api/v1/enrollments/{id}/promote", ENROLLMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROMOTE_REQUEST_JSON)
                            .with(authentication(authWithRole("PROFESSOR"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("STUDENT cannot promote student")
        void student_cannotPromote() throws Exception {
            mockMvc.perform(post("/api/v1/enrollments/{id}/promote", ENROLLMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROMOTE_REQUEST_JSON)
                            .with(authentication(authWithRole("STUDENT"))))
                    .andExpect(status().isForbidden());
        }
    }
}
