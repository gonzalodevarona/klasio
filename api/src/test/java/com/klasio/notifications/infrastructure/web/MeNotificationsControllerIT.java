package com.klasio.notifications.infrastructure.web;

import com.klasio.notifications.application.dto.NotificationView;
import com.klasio.notifications.application.port.input.GetUnreadCountUseCase;
import com.klasio.notifications.application.port.input.ListMyNotificationsUseCase;
import com.klasio.notifications.application.port.input.MarkAllNotificationsReadUseCase;
import com.klasio.notifications.application.port.input.MarkNotificationReadUseCase;
import com.klasio.notifications.domain.model.NotificationType;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import com.klasio.shared.infrastructure.exception.NotificationNotFoundException;
import com.klasio.shared.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MeNotificationsController.class)
@Import({GlobalExceptionHandler.class, MeNotificationsControllerIT.TestSecurityConfig.class})
class MeNotificationsControllerIT {

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

    @MockitoBean
    private ListMyNotificationsUseCase listUseCase;
    @MockitoBean
    private GetUnreadCountUseCase unreadCountUseCase;
    @MockitoBean
    private MarkNotificationReadUseCase markReadUseCase;
    @MockitoBean
    private MarkAllNotificationsReadUseCase markAllReadUseCase;

    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID NOTIF_ID  = UUID.randomUUID();
    private static final UUID OTHER_ID  = UUID.randomUUID();

    // ---------------------------------------------------------------
    // Auth helper
    // ---------------------------------------------------------------

    private static UsernamePasswordAuthenticationToken userAuth() {
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

    // ---------------------------------------------------------------
    // Sample data
    // ---------------------------------------------------------------

    private static NotificationView sampleView(boolean read) {
        return new NotificationView(
                NOTIF_ID,
                NotificationType.CLASS_SESSION_ALERTED,
                "Class Alert",
                "Your class starts soon",
                Map.of("classId", UUID.randomUUID().toString()),
                read ? Instant.now() : null,
                Instant.now()
        );
    }

    private static ListMyNotificationsUseCase.Result pageOf(List<NotificationView> items) {
        return new ListMyNotificationsUseCase.Result(items, items.size(), 0, 20);
    }

    // ---------------------------------------------------------------
    // 1. GET /api/v1/me/notifications returns user's notifications
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/me/notifications — returns 200 with notifications list")
    void list_authenticated_returns200() throws Exception {
        when(listUseCase.execute(eq(TENANT_ID), eq(USER_ID), eq(false), eq(0), eq(20)))
                .thenReturn(pageOf(List.of(sampleView(false))));

        mockMvc.perform(get("/api/v1/me/notifications")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(NOTIF_ID.toString()))
                .andExpect(jsonPath("$.items[0].type").value("CLASS_SESSION_ALERTED"))
                .andExpect(jsonPath("$.items[0].title").value("Class Alert"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    // ---------------------------------------------------------------
    // 2. GET /api/v1/me/notifications?unread=true excludes read ones
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/me/notifications?unread=true — forwards unreadOnly=true to use case")
    void list_unreadFilter_passesUnreadOnlyTrue() throws Exception {
        when(listUseCase.execute(eq(TENANT_ID), eq(USER_ID), eq(true), eq(0), eq(20)))
                .thenReturn(pageOf(List.of(sampleView(false))));

        mockMvc.perform(get("/api/v1/me/notifications")
                        .param("unread", "true")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].type").value("CLASS_SESSION_ALERTED"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/me/notifications?unread=true — returns empty list when all are read")
    void list_unreadFilter_emptyWhenAllRead() throws Exception {
        when(listUseCase.execute(eq(TENANT_ID), eq(USER_ID), eq(true), eq(0), eq(20)))
                .thenReturn(pageOf(List.of()));

        mockMvc.perform(get("/api/v1/me/notifications")
                        .param("unread", "true")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    // ---------------------------------------------------------------
    // 3. GET /api/v1/me/notifications/unread-count returns correct count
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/me/notifications/unread-count — returns correct count")
    void unreadCount_returns200WithCount() throws Exception {
        when(unreadCountUseCase.execute(TENANT_ID, USER_ID)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/me/notifications/unread-count")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/me/notifications/unread-count — returns 0 when none unread")
    void unreadCount_returnsZeroWhenNoneUnread() throws Exception {
        when(unreadCountUseCase.execute(TENANT_ID, USER_ID)).thenReturn(0L);

        mockMvc.perform(get("/api/v1/me/notifications/unread-count")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    // ---------------------------------------------------------------
    // 4. PATCH /api/v1/me/notifications/{id}/read returns 204
    // ---------------------------------------------------------------

    @Test
    @DisplayName("PATCH /api/v1/me/notifications/{id}/read — returns 204 on success")
    void markRead_ownNotification_returns204() throws Exception {
        doNothing().when(markReadUseCase).execute(TENANT_ID, USER_ID, NOTIF_ID);

        mockMvc.perform(patch("/api/v1/me/notifications/{id}/read", NOTIF_ID)
                        .with(authentication(userAuth())))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    // ---------------------------------------------------------------
    // 5. PATCH /api/v1/me/notifications/{otherId}/read returns 404 (cross-user)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("PATCH /api/v1/me/notifications/{id}/read — returns 404 for other user's notification")
    void markRead_otherUsersNotification_returns404() throws Exception {
        doThrow(new NotificationNotFoundException(OTHER_ID))
                .when(markReadUseCase).execute(eq(TENANT_ID), eq(USER_ID), eq(OTHER_ID));

        mockMvc.perform(patch("/api/v1/me/notifications/{id}/read", OTHER_ID)
                        .with(authentication(userAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOTIFICATION_NOT_FOUND"));
    }

    // ---------------------------------------------------------------
    // 6. POST /api/v1/me/notifications/mark-all-read returns 204
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/me/notifications/mark-all-read — returns 204")
    void markAllRead_returns204() throws Exception {
        when(markAllReadUseCase.execute(TENANT_ID, USER_ID)).thenReturn(2);

        mockMvc.perform(post("/api/v1/me/notifications/mark-all-read")
                        .with(authentication(userAuth())))
                .andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------
    // 7. Unauthenticated requests are rejected
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/me/notifications — unauthenticated returns 403")
    void list_unauthenticated_returns403() throws Exception {
        // In stateless @WebMvcTest slice without an AuthenticationEntryPoint configured,
        // unauthenticated requests result in 403 (AccessDeniedException) not 401.
        mockMvc.perform(get("/api/v1/me/notifications"))
                .andExpect(status().isForbidden());
    }
}
