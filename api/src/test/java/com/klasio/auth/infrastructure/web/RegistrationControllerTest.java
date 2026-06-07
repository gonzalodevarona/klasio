package com.klasio.auth.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klasio.auth.application.service.RegisterStudentService;
import com.klasio.auth.domain.exception.SelfRegistrationConflictException;
import com.klasio.auth.domain.exception.SelfRegistrationDisabledException;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RegistrationController.class)
@Import({GlobalExceptionHandler.class, RegistrationControllerTest.TestSecurityConfig.class})
class RegistrationControllerTest {

    @TestConfiguration
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
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
    private RegisterStudentService registerStudentService;

    private Map<String, Object> fullRequestBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "Ana");
        body.put("lastName", "Martinez");
        body.put("dateOfBirth", "1998-03-20");
        body.put("identityDocumentType", "CC");
        body.put("identityNumber", "12345678");
        body.put("eps", "Sanitas");
        body.put("email", "ana@example.com");
        body.put("bloodType", "O+");
        body.put("phone", "3001234567");
        body.put("tutorFirstName", "Pedro");
        body.put("tutorLastName", "Martinez");
        body.put("tutorRelationship", "Father");
        body.put("tutorPhone", "3009999999");
        body.put("tutorEmail", "pedro@example.com");
        return body;
    }

    @Test
    @DisplayName("returns 202 Accepted for a valid full registration body")
    void register_accepts202_withFullStructuredBody() throws Exception {
        doNothing().when(registerStudentService).register(any());

        mockMvc.perform(post("/api/v1/tenants/liga-test/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullRequestBody())))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("returns 403 with SELF_REGISTRATION_DISABLED code when league has it off")
    void register_returns403_whenDisabled() throws Exception {
        doThrow(new SelfRegistrationDisabledException())
                .when(registerStudentService).register(any());

        mockMvc.perform(post("/api/v1/tenants/liga-test/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullRequestBody())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SELF_REGISTRATION_DISABLED"));
    }

    @Test
    @DisplayName("returns 409 with REGISTRATION_FAILED code on any conflict (non-enumerating)")
    void register_returns409Generic_onConflict() throws Exception {
        doThrow(new SelfRegistrationConflictException())
                .when(registerStudentService).register(any());

        mockMvc.perform(post("/api/v1/tenants/liga-test/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullRequestBody())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_FAILED"));
    }
}
