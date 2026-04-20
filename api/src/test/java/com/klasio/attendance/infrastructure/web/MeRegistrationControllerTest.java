package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.AttendanceRegistrationView;
import com.klasio.attendance.application.port.input.CancelRegistrationUseCase;
import com.klasio.attendance.application.port.input.GetMyRegistrationUseCase;
import com.klasio.attendance.application.port.input.ListMyRegistrationsUseCase;
import com.klasio.attendance.application.port.input.RegisterForClassUseCase;
import com.klasio.attendance.domain.port.ClassDetailsPort;

import javax.sql.DataSource;
import com.klasio.shared.infrastructure.exception.CancellationWindowExpiredException;
import com.klasio.shared.infrastructure.exception.RegistrationNotFoundException;
import com.klasio.shared.infrastructure.exception.RegistrationNotCancellableException;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationId;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.membership.domain.port.StudentIdPort;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MeRegistrationController.class)
@Import({GlobalExceptionHandler.class, MeRegistrationControllerTest.TestSecurityConfig.class})
class MeRegistrationControllerTest {

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
                throw new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "Test: no JDBC user store");
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private RegisterForClassUseCase registerUseCase;
    @MockitoBean private CancelRegistrationUseCase cancelUseCase;
    @MockitoBean private ListMyRegistrationsUseCase listUseCase;
    @MockitoBean private GetMyRegistrationUseCase getUseCase;
    @MockitoBean private StudentIdPort studentIdPort;
    @MockitoBean private ClassDetailsPort classDetailsPort;
    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CLASS_ID   = UUID.randomUUID();
    private static final UUID REG_ID     = UUID.randomUUID();

    // ------------------------------------------------------------------
    // Auth helpers
    // ------------------------------------------------------------------

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

    private MockHttpServletRequestBuilder withStudentAuth(MockHttpServletRequestBuilder req) {
        return req.with(authentication(authWithRole("STUDENT")));
    }

    // ------------------------------------------------------------------
    // Sample response data
    // ------------------------------------------------------------------

    private AttendanceRegistration stubRegistration() {
        return AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(REG_ID),
                TENANT_ID,
                UUID.randomUUID(),   // sessionId
                CLASS_ID,
                STUDENT_ID,
                UUID.randomUUID(),   // enrollmentId
                UUID.randomUUID(),   // membershipId
                "BEGINNER",
                1,
                AttendanceRegistrationStatus.REGISTERED,
                LocalDate.now().plusDays(5),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                null, null,         // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), USER_ID,
                null, null
        );
    }

    private AttendanceRegistrationView stubView() {
        return new AttendanceRegistrationView(
                REG_ID,
                UUID.randomUUID(),
                CLASS_ID,
                "Yoga Beginners",
                STUDENT_ID,
                LocalDate.now().plusDays(5),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "BEGINNER",
                1,
                "REGISTERED",
                Instant.now(),
                null,
                null,
                null
        );
    }

    private static final String REGISTER_BODY = """
            {
              "classId": "%s",
              "sessionDate": "%s",
              "intendedHours": 1
            }
            """.formatted(CLASS_ID, LocalDate.now().plusDays(5));

    // ------------------------------------------------------------------
    // POST /api/v1/me/registrations
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/me/registrations — STUDENT with valid body returns 201")
    void register_asStudent_returns201() throws Exception {
        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(STUDENT_ID));
        when(registerUseCase.execute(any())).thenReturn(stubRegistration());

        mockMvc.perform(withStudentAuth(
                        post("/api/v1/me/registrations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(REGISTER_BODY)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/me/registrations — non-STUDENT role returns 403")
    void register_asAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/me/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY)
                        .with(authentication(authWithRole("ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/me/registrations — MANAGER role returns 403")
    void register_asManager_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/me/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY)
                        .with(authentication(authWithRole("MANAGER"))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // GET /api/v1/me/registrations
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/me/registrations — STUDENT returns 200")
    void list_asStudent_returns200() throws Exception {
        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(STUDENT_ID));
        when(listUseCase.execute(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stubView())));

        mockMvc.perform(withStudentAuth(get("/api/v1/me/registrations")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/me/registrations — non-STUDENT returns 403")
    void list_asProfessor_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/me/registrations")
                        .with(authentication(authWithRole("PROFESSOR"))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // GET /api/v1/me/registrations/{id}
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/me/registrations/{id} — STUDENT returns 200")
    void get_asStudent_returns200() throws Exception {
        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(STUDENT_ID));
        when(getUseCase.execute(TENANT_ID, STUDENT_ID, REG_ID))
                .thenReturn(stubView());

        mockMvc.perform(withStudentAuth(
                        get("/api/v1/me/registrations/{id}", REG_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/me/registrations/{id} — non-STUDENT returns 403")
    void getById_asAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/me/registrations/{id}", REG_ID)
                        .with(authentication(authWithRole("ADMIN"))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // DELETE /api/v1/me/registrations/{id}
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/v1/me/registrations/{id} — STUDENT returns 204")
    void cancel_asStudent_returns204() throws Exception {
        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(STUDENT_ID));
        doNothing().when(cancelUseCase).execute(any());

        mockMvc.perform(withStudentAuth(delete("/api/v1/me/registrations/{id}", REG_ID)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/me/registrations/{id} — non-STUDENT returns 403")
    void cancel_asAdmin_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/me/registrations/{id}", REG_ID)
                        .with(authentication(authWithRole("ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/me/registrations/{id} — not found returns 404")
    void cancel_notFound_returns404() throws Exception {
        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(STUDENT_ID));
        doThrow(new RegistrationNotFoundException(REG_ID))
                .when(cancelUseCase).execute(any());

        mockMvc.perform(withStudentAuth(delete("/api/v1/me/registrations/{id}", REG_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/me/registrations/{id} — window expired returns 409")
    void cancel_windowExpired_returns409() throws Exception {
        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(STUDENT_ID));
        doThrow(new CancellationWindowExpiredException(10))
                .when(cancelUseCase).execute(any());

        mockMvc.perform(withStudentAuth(delete("/api/v1/me/registrations/{id}", REG_ID)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/v1/me/registrations/{id} — not cancellable status returns 409")
    void cancel_notCancellable_returns409() throws Exception {
        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(STUDENT_ID));
        doThrow(new RegistrationNotCancellableException("CANCELLED_BY_STUDENT"))
                .when(cancelUseCase).execute(any());

        mockMvc.perform(withStudentAuth(delete("/api/v1/me/registrations/{id}", REG_ID)))
                .andExpect(status().isConflict());
    }
}
