package com.klasio.auth.application;

import com.klasio.auth.application.port.EmailVerificationTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.application.service.VerifyEmailService;
import com.klasio.auth.domain.exception.VerificationTokenAlreadyUsedException;
import com.klasio.auth.domain.exception.VerificationTokenExpiredException;
import com.klasio.auth.domain.model.EmailVerificationToken;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
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
import com.klasio.auth.domain.event.EmailVerifiedEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyEmailServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationTokenRepository evtRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;

    private VerifyEmailService service;

    @BeforeEach
    void setUp() {
        service = new VerifyEmailService(userRepository, evtRepository, tokenGenerator, eventPublisher);
    }

    @Test
    void validToken_setsUserStatusToActive() {
        UUID userId = UUID.randomUUID();
        String rawToken = "valid-raw-token";
        String hashedToken = "hashed-token";

        EmailVerificationToken token = EmailVerificationToken.create(
                userId, hashedToken, Instant.now().plusSeconds(86400));

        User user = User.createUnverified(UUID.randomUUID(), "student@example.com", "hashed-pwd",
                com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678");

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(evtRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.verify(rawToken);

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(token.isUsed());
        verify(evtRepository).save(token);
        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(any(EmailVerifiedEvent.class));
    }

    @Test
    void expiredToken_throwsVerificationTokenExpiredException() {
        String rawToken = "expired-raw-token";
        String hashedToken = "hashed-expired";

        EmailVerificationToken token = new EmailVerificationToken(
                UUID.randomUUID(), UUID.randomUUID(), hashedToken,
                Instant.now().minusSeconds(1), false, Instant.now().minusSeconds(86400));

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(evtRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));

        assertThrows(VerificationTokenExpiredException.class, () -> service.verify(rawToken));

        verify(userRepository, never()).save(any());
    }

    @Test
    void alreadyUsedToken_throwsVerificationTokenAlreadyUsedException() {
        String rawToken = "used-raw-token";
        String hashedToken = "hashed-used";

        EmailVerificationToken token = new EmailVerificationToken(
                UUID.randomUUID(), UUID.randomUUID(), hashedToken,
                Instant.now().plusSeconds(86400), true, Instant.now().minusSeconds(3600));

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(evtRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));

        assertThrows(VerificationTokenAlreadyUsedException.class, () -> service.verify(rawToken));

        verify(userRepository, never()).save(any());
    }

    @Test
    void unknownToken_throwsIllegalArgumentException() {
        String rawToken = "unknown-token";
        String hashedToken = "hashed-unknown";

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(evtRepository.findByTokenHash(hashedToken)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.verify(rawToken));
    }
}
