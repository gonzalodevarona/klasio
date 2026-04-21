package com.klasio.auth.application;

import com.klasio.auth.application.port.*;
import com.klasio.auth.application.service.ResendVerificationEmailService;
import com.klasio.auth.domain.event.VerificationEmailResendRequested;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import com.klasio.auth.infrastructure.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendVerificationEmailServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationTokenRepository evtRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ResendVerificationEmailService service;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = new AuthProperties(24, 30, 5, 15, "noreply@klasio.com");
        service = new ResendVerificationEmailService(
                userRepository, evtRepository, tokenGenerator, authProperties, eventPublisher);
    }

    @Test
    void unverifiedUser_invalidatesOldTokensAndSendsNew() {
        UUID tenantId = UUID.randomUUID();
        String email = "student@example.com";
        User user = User.createUnverified(tenantId, email, "hashed-pwd", com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenGenerator.generateRawToken()).thenReturn("new-raw-token");
        when(tokenGenerator.hashToken("new-raw-token")).thenReturn("new-hashed-token");

        service.resend(email, "test-league");

        verify(evtRepository).invalidateAllByUserId(user.getId());
        verify(evtRepository).save(any());

        ArgumentCaptor<VerificationEmailResendRequested> eventCaptor =
                ArgumentCaptor.forClass(VerificationEmailResendRequested.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        VerificationEmailResendRequested event = eventCaptor.getValue();
        assertEquals(email, event.email());
        assertEquals("test-league", event.tenantSlug());
        assertEquals("new-raw-token", event.rawToken());
        // createUnverified sets firstName/lastName to null, so displayName falls back to email
        assertEquals(email, event.displayName());
        assertNotNull(event.expiresAt());
    }

    @Test
    void unknownEmail_noOp_noException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.resend("unknown@example.com", "test-league");

        verify(evtRepository, never()).invalidateAllByUserId(any());
        verify(evtRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void alreadyVerifiedUser_noOp_noException() {
        UUID tenantId = UUID.randomUUID();
        User user = User.createActive(tenantId, "verified@example.com", "hashed-pwd",
                com.klasio.auth.domain.model.Role.STUDENT, com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678", null, null, null);

        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(user));

        service.resend("verified@example.com", "test-league");

        verify(evtRepository, never()).invalidateAllByUserId(any());
        verify(evtRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
