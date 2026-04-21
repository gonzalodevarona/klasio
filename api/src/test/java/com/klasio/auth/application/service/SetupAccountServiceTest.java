package com.klasio.auth.application.service;

import com.klasio.auth.application.port.AccountSetupTokenRepository;
import com.klasio.auth.application.port.PasswordEncoder;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.AccountSetupCompletedEvent;
import com.klasio.auth.domain.exception.AccountSetupTokenAlreadyUsedException;
import com.klasio.auth.domain.exception.AccountSetupTokenExpiredException;
import com.klasio.auth.domain.exception.AccountSetupTokenInvalidException;
import com.klasio.auth.domain.exception.PasswordPolicyViolationException;
import com.klasio.auth.domain.model.AccountSetupToken;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.shared.domain.model.IdentityDocumentType;
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

@ExtendWith(MockitoExtension.class)
class SetupAccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountSetupTokenRepository accountSetupTokenRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;

    private SetupAccountService service;

    @BeforeEach
    void setUp() {
        service = new SetupAccountService(
                userRepository, accountSetupTokenRepository, tokenGenerator,
                passwordEncoder, eventPublisher);
    }

    @Test
    void setup_withValidToken_activatesUserAndSetsPassword() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String rawToken = "valid-raw-token";
        String hashedToken = "hashed-valid-token";
        String newPassword = "NewSecure1!";
        String encodedPassword = "encoded-new-password";

        AccountSetupToken token = AccountSetupToken.create(userId, hashedToken,
                Instant.now().plusSeconds(900));
        User user = User.createPendingSetup(tenantId, "student@example.com", Role.STUDENT,
                IdentityDocumentType.CC, "12345678", "John", "Doe", null);

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(accountSetupTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setup(rawToken, newPassword);

        assertEquals(encodedPassword, user.getPasswordHash());
        assertTrue(token.isUsed());
        verify(accountSetupTokenRepository).save(token);
        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(any(AccountSetupCompletedEvent.class));
    }

    @Test
    void setup_withExpiredToken_throwsAccountSetupTokenExpiredException() {
        String rawToken = "expired-token";
        String hashedToken = "hashed-expired";

        AccountSetupToken token = new AccountSetupToken(
                UUID.randomUUID(), UUID.randomUUID(), hashedToken,
                Instant.now().minusSeconds(1), false, Instant.now().minusSeconds(900));

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(accountSetupTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));

        assertThrows(AccountSetupTokenExpiredException.class,
                () -> service.setup(rawToken, "NewSecure1!"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void setup_withUsedToken_throwsAccountSetupTokenAlreadyUsedException() {
        String rawToken = "used-token";
        String hashedToken = "hashed-used";

        AccountSetupToken token = new AccountSetupToken(
                UUID.randomUUID(), UUID.randomUUID(), hashedToken,
                Instant.now().plusSeconds(900), true, Instant.now().minusSeconds(60));

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(accountSetupTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));

        assertThrows(AccountSetupTokenAlreadyUsedException.class,
                () -> service.setup(rawToken, "NewSecure1!"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void setup_withInvalidToken_throwsAccountSetupTokenInvalidException() {
        String rawToken = "nonexistent-token";
        String hashedToken = "hashed-nonexistent";

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(accountSetupTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.empty());

        assertThrows(AccountSetupTokenInvalidException.class,
                () -> service.setup(rawToken, "NewSecure1!"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void setup_withWeakPassword_throwsPasswordPolicyViolationException() {
        UUID userId = UUID.randomUUID();
        String rawToken = "valid-token";
        String hashedToken = "hashed-valid";

        AccountSetupToken token = AccountSetupToken.create(userId, hashedToken,
                Instant.now().plusSeconds(900));

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(accountSetupTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));

        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class,
                () -> service.setup(rawToken, "weak"));
        assertFalse(ex.getViolations().isEmpty());
        verify(userRepository, never()).save(any());
    }
}
