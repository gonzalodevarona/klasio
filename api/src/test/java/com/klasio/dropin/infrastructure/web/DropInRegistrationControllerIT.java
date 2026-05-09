package com.klasio.dropin.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klasio.dropin.application.dto.RegisterDropInResult;
import com.klasio.dropin.application.service.RegisterDropInService;
import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import com.klasio.shared.infrastructure.exception.PhoneAlreadyExistsException;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DropInRegistrationController.class)
@Import({GlobalExceptionHandler.class, DropInRegistrationControllerIT.TestSecurityConfig.class})
class DropInRegistrationControllerIT {

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
    @Autowired ObjectMapper objectMapper;
    @MockitoBean RegisterDropInService registerDropInService;

    private static final UUID TENANT_ID   = UUID.randomUUID();
    private static final UUID CLASS_ID    = UUID.randomUUID();
    private static final UUID USER_ID     = UUID.randomUUID();
    private static final UUID PROGRAM_ID  = UUID.randomUUID();
    private static final LocalDate SESSION_DATE = LocalDate.of(2026, 5, 15);

    private static final String DROP_IN_URL =
            "/api/v1/classes/{classId}/sessions/{sessionDate}/drop-in";

    private UsernamePasswordAuthenticationToken authWithRole(String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        Map<String, Object> details = new HashMap<>();
        details.put("userId", USER_ID.toString());
        details.put("tenantId", TENANT_ID.toString());
        details.put("programId", PROGRAM_ID.toString());
        details.put("role", role);
        auth.setDetails(details);
        return auth;
    }

    private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req, String role) {
        return req.with(authentication(authWithRole(role)));
    }

    // ------------------------------------------------------------------
    // Test 1: POST with valid new-attendee body → 201
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST with new-attendee body returns 201 with attendeeWasNew=true")
    void post_newAttendee_returns201() throws Exception {
        UUID regId      = UUID.randomUUID();
        UUID attendeeId = UUID.randomUUID();
        UUID paymentId  = UUID.randomUUID();

        when(registerDropInService.execute(any()))
                .thenReturn(new RegisterDropInResult(regId, attendeeId, paymentId, true, 1));

        var body = Map.of(
                "startTime", "18:00",
                "attendee", Map.of(
                        "newAttendee", Map.of(
                                "fullName", "Jane Doe",
                                "phone", "3001234567"
                        )
                ),
                "amount", new BigDecimal("15.00"),
                "paymentMethod", "CASH"
        );

        mockMvc.perform(withAuth(
                        post(DROP_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "ADMIN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationId").value(regId.toString()))
                .andExpect(jsonPath("$.attendeeId").value(attendeeId.toString()))
                .andExpect(jsonPath("$.attendeeWasNew").value(true))
                .andExpect(jsonPath("$.status").value("PRESENT"))
                .andExpect(jsonPath("$.attendeeTotalVisits").value(1));
    }

    // ------------------------------------------------------------------
    // Test 2: POST with valid existing-attendee body → 200
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST with existingId body returns 200 with attendeeWasNew=false")
    void post_existingAttendee_returns200() throws Exception {
        UUID regId      = UUID.randomUUID();
        UUID attendeeId = UUID.randomUUID();
        UUID paymentId  = UUID.randomUUID();

        when(registerDropInService.execute(any()))
                .thenReturn(new RegisterDropInResult(regId, attendeeId, paymentId, false, 5));

        var body = Map.of(
                "startTime", "09:30",
                "attendee", Map.of(
                        "existingId", attendeeId.toString()
                ),
                "amount", new BigDecimal("15.00"),
                "paymentMethod", "TRANSFER"
        );

        mockMvc.perform(withAuth(
                        post(DROP_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "PROFESSOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendeeWasNew").value(false))
                .andExpect(jsonPath("$.attendeeTotalVisits").value(5));
    }

    // ------------------------------------------------------------------
    // Test 3: POST unauthenticated → 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST without authentication returns 403")
    void post_unauthenticated_returns403() throws Exception {
        var body = Map.of(
                "startTime", "18:00",
                "attendee", Map.of("existingId", UUID.randomUUID().toString()),
                "amount", new BigDecimal("15.00"),
                "paymentMethod", "CASH"
        );

        mockMvc.perform(
                        post(DROP_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Test 4: POST with STUDENT role → 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST with STUDENT role returns 403")
    void post_studentRole_returns403() throws Exception {
        var body = Map.of(
                "startTime", "18:00",
                "attendee", Map.of("existingId", UUID.randomUUID().toString()),
                "amount", new BigDecimal("15.00"),
                "paymentMethod", "CASH"
        );

        mockMvc.perform(withAuth(
                        post(DROP_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "STUDENT"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Test 5: POST with both attendee branches null → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST with both attendee fields null returns 400")
    void post_bothAttendeeBranchesNull_returns400() throws Exception {
        // attendee object with neither existingId nor newAttendee
        var body = new HashMap<String, Object>();
        body.put("startTime", "18:00");
        body.put("attendee", new HashMap<>());  // existingId=null, newAttendee=null
        body.put("amount", new BigDecimal("15.00"));
        body.put("paymentMethod", "CASH");

        mockMvc.perform(withAuth(
                        post(DROP_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "ADMIN"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Test 6: POST with both attendee branches set → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST with both existingId and newAttendee set returns 400")
    void post_bothAttendeeBranchesSet_returns400() throws Exception {
        var newAttendee = Map.of("fullName", "Jane Doe", "phone", "3001234567");
        var attendee = new HashMap<String, Object>();
        attendee.put("existingId", UUID.randomUUID().toString());
        attendee.put("newAttendee", newAttendee);

        var body = new HashMap<String, Object>();
        body.put("startTime", "18:00");
        body.put("attendee", attendee);
        body.put("amount", new BigDecimal("15.00"));
        body.put("paymentMethod", "CASH");

        mockMvc.perform(withAuth(
                        post(DROP_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "ADMIN"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Test 7: POST with malformed startTime → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST with invalid startTime format returns 400")
    void post_malformedStartTime_returns400() throws Exception {
        var body = new HashMap<String, Object>();
        body.put("startTime", "99:99");  // invalid
        body.put("attendee", Map.of("existingId", UUID.randomUUID().toString()));
        body.put("amount", new BigDecimal("15.00"));
        body.put("paymentMethod", "CASH");

        mockMvc.perform(withAuth(
                        post(DROP_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "ADMIN"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Test 8: POST with amount = 0 → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST with amount 0 returns 400")
    void post_amountZero_returns400() throws Exception {
        var body = new HashMap<String, Object>();
        body.put("startTime", "18:00");
        body.put("attendee", Map.of("existingId", UUID.randomUUID().toString()));
        body.put("amount", new BigDecimal("0.00"));
        body.put("paymentMethod", "CASH");

        mockMvc.perform(withAuth(
                        post(DROP_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "ADMIN"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Test 9: POST with blank fullName → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST with blank fullName in newAttendee returns 400")
    void post_blankFullName_returns400() throws Exception {
        var newAttendee = Map.of("fullName", "   ", "phone", "3001234567");
        var body = new HashMap<String, Object>();
        body.put("startTime", "18:00");
        body.put("attendee", Map.of("newAttendee", newAttendee));
        body.put("amount", new BigDecimal("15.00"));
        body.put("paymentMethod", "CASH");

        mockMvc.perform(withAuth(
                        post(DROP_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "MANAGER"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Test 10: POST when service throws PhoneAlreadyExistsException → 409
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST when phone already exists returns 409 with conflict details")
    void post_phoneAlreadyExists_returns409() throws Exception {
        UUID existingId = UUID.randomUUID();
        when(registerDropInService.execute(any()))
                .thenThrow(new PhoneAlreadyExistsException(existingId, "Jane Doe", 3));

        var body = new HashMap<String, Object>();
        body.put("startTime", "18:00");
        body.put("attendee", Map.of(
                "newAttendee", Map.of("fullName", "Jane Doe", "phone", "3001234567")
        ));
        body.put("amount", new BigDecimal("15.00"));
        body.put("paymentMethod", "CASH");

        mockMvc.perform(withAuth(
                        post(DROP_IN_URL, CLASS_ID, SESSION_DATE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)),
                        "ADMIN"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.existingAttendeeId").value(existingId.toString()))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.totalVisits").value(3));
    }
}
