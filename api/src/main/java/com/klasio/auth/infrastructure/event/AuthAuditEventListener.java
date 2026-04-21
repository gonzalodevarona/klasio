package com.klasio.auth.infrastructure.event;

import com.klasio.audit.domain.model.AuditLogEntry;
import com.klasio.audit.infrastructure.persistence.JpaAuditLogRepository;
import com.klasio.auth.domain.event.EmailVerifiedEvent;
import com.klasio.auth.domain.event.PasswordResetCompletedEvent;
import com.klasio.auth.domain.event.PasswordResetRequestedEvent;
import com.klasio.auth.domain.event.RoleAssignedEvent;
import com.klasio.auth.domain.event.UserAccountLockedEvent;
import com.klasio.auth.domain.event.UserLoggedInEvent;
import com.klasio.auth.domain.event.UserLoggedOutEvent;
import com.klasio.auth.domain.event.UserLoginFailedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class AuthAuditEventListener {

    private final JpaAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuthAuditEventListener(JpaAuditLogRepository auditLogRepository,
                                   ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @EventListener
    public void onUserLoggedIn(UserLoggedInEvent event) {
        log.info("Auth audit: user logged in userId={}, role={}", event.userId(), event.role());

        String details = toJson(Map.of(
                "email", event.email(),
                "role", event.role().name()));

        saveEntry("AUTH_LOGIN", event.userId(), "USER", event.userId(), event.occurredAt(), details);
    }

    @Async
    @EventListener
    public void onUserLoginFailed(UserLoginFailedEvent event) {
        log.info("Auth audit: login failed email={}, attempts={}", event.email(), event.failedAttempts());

        HashMap<String, String> detailMap = new HashMap<>();
        detailMap.put("email", event.email());
        detailMap.put("failedAttempts", String.valueOf(event.failedAttempts()));
        String details = toJson(detailMap);

        saveEntry("AUTH_LOGIN_FAILED", event.userId(), "USER", event.userId(), event.occurredAt(), details);
    }

    @Async
    @EventListener
    public void onUserAccountLocked(UserAccountLockedEvent event) {
        log.info("Auth audit: account locked userId={}, until={}", event.userId(), event.lockedUntil());

        String details = toJson(Map.of(
                "email", event.email(),
                "lockedUntil", event.lockedUntil().toString()));

        saveEntry("AUTH_ACCOUNT_LOCKED", event.userId(), "USER", event.userId(), event.occurredAt(), details);
    }

    @Async
    @EventListener
    public void onUserLoggedOut(UserLoggedOutEvent event) {
        log.info("Auth audit: user logged out userId={}", event.userId());

        saveEntry("AUTH_LOGOUT", event.userId(), "USER", event.userId(), event.occurredAt(), "{}");
    }

    @Async
    @EventListener
    public void onEmailVerified(EmailVerifiedEvent event) {
        log.info("Auth audit: email verified userId={}", event.userId());

        String details = toJson(Map.of("email", event.email()));

        saveEntry("AUTH_EMAIL_VERIFIED", event.userId(), "USER", event.userId(), event.occurredAt(), details);
    }

    @Async
    @EventListener
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Auth audit: password reset requested userId={}", event.userId());

        String details = toJson(Map.of("email", event.email()));

        saveEntry("AUTH_PASSWORD_RESET_REQUESTED", event.userId(), "USER", event.userId(), event.occurredAt(), details);
    }

    @Async
    @EventListener
    public void onPasswordResetCompleted(PasswordResetCompletedEvent event) {
        log.info("Auth audit: password reset completed userId={}", event.userId());

        saveEntry("AUTH_PASSWORD_RESET_COMPLETED", event.userId(), "USER", event.userId(), event.occurredAt(), "{}");
    }

    @Async
    @EventListener
    public void onRoleAssigned(RoleAssignedEvent event) {
        log.info("Auth audit: role assigned userId={}, newRole={}", event.userId(), event.newRole());

        String details = toJson(Map.of(
                "userId", event.userId().toString(),
                "previousRole", event.previousRole().name(),
                "newRole", event.newRole().name()));

        saveEntry("ROLE_ASSIGNED", event.assignedBy(), "USER", event.userId(), event.occurredAt(), details);
    }

    private void saveEntry(String action, UUID actorId, String entityType, UUID entityId,
                           java.time.Instant occurredAt, String details) {
        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(), action, actorId, entityType, entityId, occurredAt, details);
        auditLogRepository.save(entry);
    }

    private String toJson(Map<String, String> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize auth audit details", ex);
            return "{}";
        }
    }
}
