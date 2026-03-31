package com.klasio.auth.application;

import com.klasio.auth.application.port.*;
import com.klasio.auth.application.service.ResendVerificationEmailService;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import com.klasio.auth.infrastructure.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendVerificationEmailServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationTokenRepository evtRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private AuthEmailSender authEmailSender;

    private ResendVerificationEmailService service;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = new AuthProperties(24, 30, 5, 15, "noreply@klasio.com");
        service = new ResendVerificationEmailService(
                userRepository, evtRepository, tokenGenerator, authEmailSender, authProperties);
    }

    @Test
    void unverifiedUser_invalidatesOldTokensAndSendsNew() {
        UUID tenantId = UUID.randomUUID();
        String email = "student@example.com";
        User user = User.createUnverified(tenantId, email, "hashed-pwd");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenGenerator.generateRawToken()).thenReturn("new-raw-token");
        when(tokenGenerator.hashToken("new-raw-token")).thenReturn("new-hashed-token");

        service.resend(email, "test-league");

        verify(evtRepository).invalidateAllByUserId(user.getId());
        verify(evtRepository).save(any());
        verify(authEmailSender).sendVerificationEmail(eq(email), eq("new-raw-token"), eq("test-league"));
    }

    @Test
    void unknownEmail_noOp_noException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.resend("unknown@example.com", "test-league");

        verify(evtRepository, never()).invalidateAllByUserId(any());
        verify(evtRepository, never()).save(any());
        verify(authEmailSender, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    void alreadyVerifiedUser_noOp_noException() {
        UUID tenantId = UUID.randomUUID();
        User user = User.createActive(tenantId, "verified@example.com", "hashed-pwd",
                com.klasio.auth.domain.model.Role.STUDENT);

        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(user));

        service.resend("verified@example.com", "test-league");

        verify(evtRepository, never()).invalidateAllByUserId(any());
        verify(evtRepository, never()).save(any());
        verify(authEmailSender, never()).sendVerificationEmail(any(), any(), any());
    }
}
