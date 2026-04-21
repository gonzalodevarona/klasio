package com.klasio.student.infrastructure.web;

import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import com.klasio.student.application.port.input.CreateStudentUseCase;
import com.klasio.student.application.port.input.GetStudentUseCase;
import com.klasio.student.application.port.input.ListEnrollmentsUseCase;
import com.klasio.student.application.port.input.ListStudentsUseCase;
import com.klasio.student.application.port.input.UpdateStudentUseCase;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.StudentRepository;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StudentController.class)
@Import({GlobalExceptionHandler.class, StudentControllerRbacTest.TestSecurityConfig.class})
class StudentControllerRbacTest {

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
                            .requestMatchers("/api/v1/students/**").authenticated()
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
    private CreateStudentUseCase createStudentUseCase;

    @MockitoBean
    private GetStudentUseCase getStudentUseCase;

    @MockitoBean
    private ListStudentsUseCase listStudentsUseCase;

    @MockitoBean
    private UpdateStudentUseCase updateStudentUseCase;

    @MockitoBean
    private ListEnrollmentsUseCase listEnrollmentsUseCase;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private StudentIdPort studentIdPort;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
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

    private Student buildStudent() {
        Student student = Student.create(
                TENANT_ID, "Juan", "Perez", "juan@test.com",
                LocalDate.of(2000, 1, 1), "Sura", "123456",
                IdentityDocumentType.CC, null, null,
                null, null, null, null, null, USER_ID
        );
        student.clearDomainEvents();
        return student;
    }

    private static final String CREATE_STUDENT_JSON = """
            {
              "firstName": "Juan",
              "lastName": "Perez",
              "email": "juan@test.com",
              "dateOfBirth": "2000-01-15",
              "eps": "Sura",
              "identityNumber": "123456",
              "identityDocumentType": "CC",
              "phone": "+573001234567"
            }
            """;

    private static final String UPDATE_STUDENT_JSON = """
            {
              "firstName": "Juan Updated",
              "lastName": "Perez",
              "email": "juan@test.com",
              "dateOfBirth": "2000-01-15",
              "eps": "Sura",
              "identityNumber": "123456",
              "identityDocumentType": "CC",
              "phone": "+573001234567"
            }
            """;

    @Nested
    @DisplayName("POST /api/v1/students — Create Student RBAC")
    class CreateStudentRbac {

        @Test
        @DisplayName("ADMIN can create student")
        void admin_canCreate() throws Exception {
            when(createStudentUseCase.execute(any())).thenReturn(buildStudent());

            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_STUDENT_JSON)
                            .with(authentication(authWithRole("ADMIN"))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("SUPERADMIN can create student")
        void superadmin_canCreate() throws Exception {
            when(createStudentUseCase.execute(any())).thenReturn(buildStudent());

            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_STUDENT_JSON)
                            .with(authentication(authWithRole("SUPERADMIN"))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("MANAGER can create student")
        void manager_canCreate() throws Exception {
            when(createStudentUseCase.execute(any())).thenReturn(buildStudent());

            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_STUDENT_JSON)
                            .with(authentication(authWithRole("MANAGER"))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("PROFESSOR cannot create student")
        void professor_cannotCreate() throws Exception {
            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_STUDENT_JSON)
                            .with(authentication(authWithRole("PROFESSOR"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("STUDENT cannot create student")
        void student_cannotCreate() throws Exception {
            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_STUDENT_JSON)
                            .with(authentication(authWithRole("STUDENT"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/students/{id} — Update Student RBAC")
    class UpdateStudentRbac {

        @Test
        @DisplayName("ADMIN can update student")
        void admin_canUpdate() throws Exception {
            when(updateStudentUseCase.execute(any())).thenReturn(buildStudent());

            mockMvc.perform(put("/api/v1/students/{id}", STUDENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_STUDENT_JSON)
                            .with(authentication(authWithRole("ADMIN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("MANAGER can update student")
        void manager_canUpdate() throws Exception {
            when(updateStudentUseCase.execute(any())).thenReturn(buildStudent());

            mockMvc.perform(put("/api/v1/students/{id}", STUDENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_STUDENT_JSON)
                            .with(authentication(authWithRole("MANAGER"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PROFESSOR cannot update student")
        void professor_cannotUpdate() throws Exception {
            mockMvc.perform(put("/api/v1/students/{id}", STUDENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_STUDENT_JSON)
                            .with(authentication(authWithRole("PROFESSOR"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("STUDENT cannot update student")
        void student_cannotUpdate() throws Exception {
            mockMvc.perform(put("/api/v1/students/{id}", STUDENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_STUDENT_JSON)
                            .with(authentication(authWithRole("STUDENT"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/students/{id}/deactivate — Deactivate Student RBAC")
    class DeactivateStudentRbac {

        @Test
        @DisplayName("ADMIN can deactivate student")
        void admin_canDeactivate() throws Exception {
            Student student = buildStudent();
            when(getStudentUseCase.execute(TENANT_ID, STUDENT_ID)).thenReturn(student);

            mockMvc.perform(post("/api/v1/students/{id}/deactivate", STUDENT_ID)
                            .with(authentication(authWithRole("ADMIN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("MANAGER can deactivate student")
        void manager_canDeactivate() throws Exception {
            Student student = buildStudent();
            when(getStudentUseCase.execute(TENANT_ID, STUDENT_ID)).thenReturn(student);

            mockMvc.perform(post("/api/v1/students/{id}/deactivate", STUDENT_ID)
                            .with(authentication(authWithRole("MANAGER"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PROFESSOR cannot deactivate student")
        void professor_cannotDeactivate() throws Exception {
            mockMvc.perform(post("/api/v1/students/{id}/deactivate", STUDENT_ID)
                            .with(authentication(authWithRole("PROFESSOR"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("STUDENT cannot deactivate student")
        void student_cannotDeactivate() throws Exception {
            mockMvc.perform(post("/api/v1/students/{id}/deactivate", STUDENT_ID)
                            .with(authentication(authWithRole("STUDENT"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/students/{id}/reactivate — Reactivate Student RBAC")
    class ReactivateStudentRbac {

        @Test
        @DisplayName("MANAGER can reactivate student")
        void manager_canReactivate() throws Exception {
            Student student = buildStudent();
            student.deactivate(USER_ID);
            student.clearDomainEvents();
            when(getStudentUseCase.execute(TENANT_ID, STUDENT_ID)).thenReturn(student);

            mockMvc.perform(post("/api/v1/students/{id}/reactivate", STUDENT_ID)
                            .with(authentication(authWithRole("MANAGER"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PROFESSOR cannot reactivate student")
        void professor_cannotReactivate() throws Exception {
            mockMvc.perform(post("/api/v1/students/{id}/reactivate", STUDENT_ID)
                            .with(authentication(authWithRole("PROFESSOR"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("STUDENT cannot reactivate student")
        void student_cannotReactivate() throws Exception {
            mockMvc.perform(post("/api/v1/students/{id}/reactivate", STUDENT_ID)
                            .with(authentication(authWithRole("STUDENT"))))
                    .andExpect(status().isForbidden());
        }
    }
}
