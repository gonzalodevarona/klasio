package com.klasio.membership.infrastructure.web;

import com.klasio.membership.application.port.input.ActivateMembershipUseCase;
import com.klasio.membership.application.port.input.AdjustHoursUseCase;
import com.klasio.membership.application.port.input.CreateMembershipUseCase;
import com.klasio.membership.application.port.input.CreateSelfMembershipUseCase;
import com.klasio.membership.application.port.input.GetActiveMembershipUseCase;
import com.klasio.membership.application.port.input.GetHourTransactionsUseCase;
import com.klasio.membership.application.port.input.GetMembershipHistoryUseCase;
import com.klasio.membership.application.port.input.GetMembershipUseCase;
import com.klasio.membership.application.port.input.ListMembershipsUseCase;
import com.klasio.membership.application.port.input.RenewMembershipUseCase;
import com.klasio.membership.application.port.input.UploadPaymentProofUseCase;
import com.klasio.membership.application.port.input.ValidatePaymentUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipId;
import com.klasio.membership.domain.port.ProgramNamePort;
import com.klasio.membership.domain.port.StudentIdPort;
import com.klasio.membership.domain.port.StudentNamePort;
import com.klasio.program.domain.model.ProgramModality;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer tests for MembershipController.
 * Focus: verify that UNLIMITED plan memberships (with null hours) are accepted and
 * the response correctly serialises purchasedHours and availableHours as null.
 */
@WebMvcTest(controllers = MembershipController.class)
@Import({GlobalExceptionHandler.class, MembershipControllerTest.TestSecurityConfig.class})
class MembershipControllerTest {

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

    // All use cases required by MembershipController constructor
    @MockitoBean private CreateMembershipUseCase createMembershipUseCase;
    @MockitoBean private CreateSelfMembershipUseCase createSelfMembershipUseCase;
    @MockitoBean private RenewMembershipUseCase renewMembershipUseCase;
    @MockitoBean private ValidatePaymentUseCase validatePaymentUseCase;
    @MockitoBean private ActivateMembershipUseCase activateMembershipUseCase;
    @MockitoBean private AdjustHoursUseCase adjustHoursUseCase;
    @MockitoBean private GetMembershipUseCase getMembershipUseCase;
    @MockitoBean private ListMembershipsUseCase listMembershipsUseCase;
    @MockitoBean private GetActiveMembershipUseCase getActiveMembershipUseCase;
    @MockitoBean private GetMembershipHistoryUseCase getMembershipHistoryUseCase;
    @MockitoBean private GetHourTransactionsUseCase getHourTransactionsUseCase;
    @MockitoBean private UploadPaymentProofUseCase uploadPaymentProofUseCase;
    @MockitoBean private StudentNamePort studentNamePort;
    @MockitoBean private ProgramNamePort programNamePort;
    @MockitoBean private StudentIdPort studentIdPort;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID PLAN_ID    = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();

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
     * Builds a minimal UNLIMITED Membership via Membership.reconstitute so that
     * purchasedHours and availableHours are null (correct for UNLIMITED modality).
     */
    private Membership buildUnlimitedMembership() {
        return Membership.reconstitute(
                MembershipId.of(UUID.randomUUID()),
                TENANT_ID,
                STUDENT_ID,
                ENROLLMENT_ID,
                PROGRAM_ID,
                PLAN_ID,
                "Unlimited Monthly",
                ProgramModality.UNLIMITED,
                null,            // purchasedHours — null for UNLIMITED
                null,            // availableHours — null for UNLIMITED
                LocalDate.now(),
                LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()),
                com.klasio.membership.domain.model.MembershipStatus.PENDING_PAYMENT,
                false,
                null,
                null,
                null,
                null,
                Instant.now(),
                USER_ID,
                null,
                null
        );
    }

    @Test
    @DisplayName("POST /memberships with UNLIMITED plan returns 201 with null purchasedHours and availableHours")
    void createMembership_withUnlimitedPlan_returns201WithNullHours() throws Exception {
        Membership unlimitedMembership = buildUnlimitedMembership();
        when(createMembershipUseCase.execute(any())).thenReturn(unlimitedMembership);
        when(studentNamePort.findFullName(any(), any())).thenReturn(Optional.of("Ana Gomez"));
        when(programNamePort.findName(any(), any())).thenReturn(Optional.of("Adults Fitness"));

        String requestBody = """
                {
                  "studentId": "%s",
                  "planId": "%s",
                  "startDate": "%s",
                  "paymentValidated": false,
                  "activateDirectly": false
                }
                """.formatted(STUDENT_ID, PLAN_ID, LocalDate.now());

        mockMvc.perform(post("/api/v1/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modality").value("UNLIMITED"))
                .andExpect(jsonPath("$.purchasedHours").doesNotExist())
                .andExpect(jsonPath("$.availableHours").doesNotExist());
    }
}
