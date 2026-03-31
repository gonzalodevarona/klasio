package com.klasio.auth.application;

import com.klasio.auth.application.port.*;
import com.klasio.auth.application.service.RequestPasswordResetService;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.infrastructure.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.klasio.auth.domain.event.PasswordResetRequestedEvent;

@ExtendWith(MockitoExtension.class)
class RequestPasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository prtRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private AuthEmailSender authEmailSender;
    @Mock private ApplicationEventPublisher eventPublisher;

    private RequestPasswordResetService service;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = new AuthProperties(24, 30, 5, 15, "noreply@klasio.com");
        service = new RequestPasswordResetService(
                userRepository, prtRepository, tokenGenerator, authEmailSender,
                authProperties, eventPublisher);
    }

    @Test
    void registeredEmail_generatesTokenAndSendsEmail() {
        UUID tenantId = UUID.randomUUID();
        String email = "user@example.com";
        User user = User.createActive(tenantId, email, "hashed-pwd", Role.STUDENT);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenGenerator.generateRawToken()).thenReturn("raw-reset-token");
        when(tokenGenerator.hashToken("raw-reset-token")).thenReturn("hashed-reset-token");

        service.requestReset(email);

        verify(prtRepository).invalidateAllByUserId(user.getId());
        verify(prtRepository).save(any());
        verify(authEmailSender).sendPasswordResetEmail(eq(email), eq("raw-reset-token"));
        verify(eventPublisher).publishEvent(any(PasswordResetRequestedEvent.class));
    }

    @Test
    void unregisteredEmail_noOpNoException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.requestReset("unknown@example.com");

        verify(prtRepository, never()).invalidateAllByUserId(any());
        verify(prtRepository, never()).save(any());
        verify(authEmailSender, never()).sendPasswordResetEmail(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
