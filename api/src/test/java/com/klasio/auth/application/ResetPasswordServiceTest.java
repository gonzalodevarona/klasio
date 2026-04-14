package com.klasio.auth.application;

import com.klasio.auth.application.port.*;
import com.klasio.auth.application.service.ResetPasswordService;
import com.klasio.auth.domain.exception.PasswordPolicyViolationException;
import com.klasio.auth.domain.exception.ResetTokenAlreadyUsedException;
import com.klasio.auth.domain.exception.ResetTokenExpiredException;
import com.klasio.auth.domain.model.PasswordResetToken;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.klasio.auth.domain.event.PasswordResetCompletedEvent;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository prtRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ResetPasswordService service;

    @BeforeEach
    void setUp() {
        service = new ResetPasswordService(
                userRepository, prtRepository, tokenGenerator, passwordEncoder, eventPublisher);
    }

    @Test
    void validTokenAndCompliantPassword_resetsPassword() {
        UUID userId = UUID.randomUUID();
        String rawToken = "valid-token";
        String hashedToken = "hashed-token";
        String newPassword = "NewSecure1!";

        PasswordResetToken token = PasswordResetToken.create(userId, hashedToken,
                Instant.now().plusSeconds(1800));
        User user = User.createActive(UUID.randomUUID(), "user@example.com", "old-hash", Role.STUDENT,
                com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678");

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(prtRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reset(rawToken, newPassword);

        assertEquals("new-hash", user.getPasswordHash());
        assertTrue(token.isUsed());
        verify(prtRepository).save(token);
        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(any(PasswordResetCompletedEvent.class));
    }

    @Test
    void expiredToken_throwsResetTokenExpiredException() {
        String rawToken = "expired-token";
        String hashedToken = "hashed-expired";

        PasswordResetToken token = new PasswordResetToken(
                UUID.randomUUID(), UUID.randomUUID(), hashedToken,
                Instant.now().minusSeconds(1), false, Instant.now().minusSeconds(1800));

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(prtRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));

        assertThrows(ResetTokenExpiredException.class, () -> service.reset(rawToken, "NewSecure1!"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void usedToken_throwsResetTokenAlreadyUsedException() {
        String rawToken = "used-token";
        String hashedToken = "hashed-used";

        PasswordResetToken token = new PasswordResetToken(
                UUID.randomUUID(), UUID.randomUUID(), hashedToken,
                Instant.now().plusSeconds(1800), true, Instant.now().minusSeconds(600));

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(prtRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));

        assertThrows(ResetTokenAlreadyUsedException.class, () -> service.reset(rawToken, "NewSecure1!"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void policyViolation_throwsPasswordPolicyViolationException() {
        UUID userId = UUID.randomUUID();
        String rawToken = "valid-token";
        String hashedToken = "hashed-token";

        PasswordResetToken token = PasswordResetToken.create(userId, hashedToken,
                Instant.now().plusSeconds(1800));

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(prtRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));

        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class, () -> service.reset(rawToken, "weak"));
        assertFalse(ex.getViolations().isEmpty());
        verify(userRepository, never()).save(any());
    }

    @Test
    void unknownToken_throwsIllegalArgumentException() {
        String rawToken = "unknown";
        when(tokenGenerator.hashToken(rawToken)).thenReturn("hashed-unknown");
        when(prtRepository.findByTokenHash("hashed-unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.reset(rawToken, "NewSecure1!"));
    }
}
