package com.klasio.auth.application;

import com.klasio.auth.application.dto.LoginCommand;
import com.klasio.auth.application.dto.LoginResult;
import com.klasio.auth.application.port.PasswordEncoder;
import com.klasio.auth.application.port.RefreshTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.application.service.LoginService;
import com.klasio.auth.domain.exception.AccountLockedException;
import com.klasio.auth.domain.exception.AccountSetupPendingException;
import com.klasio.auth.domain.exception.EmailNotVerifiedException;
import com.klasio.auth.domain.exception.InvalidCredentialsException;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import com.klasio.auth.infrastructure.config.AuthProperties;
import com.klasio.auth.infrastructure.security.JwtTokenService;
import com.klasio.shared.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;

    private LoginService loginService;
    private JwtTokenService jwtTokenService;
    private AuthProperties authProperties;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProps = new JwtProperties(
                "local-dev-secret-key-that-is-at-least-256-bits-long-for-hs256-signing",
                28800000L, 604800000L);
        jwtTokenService = new JwtTokenService(jwtProps);
        authProperties = new AuthProperties(24, 30, 5, 15, "noreply@klasio.local");
        loginService = new LoginService(userRepository, passwordEncoder, refreshTokenRepository,
                tokenGenerator, jwtTokenService, authProperties, jwtProps, eventPublisher);
    }

    private User activeUser(Role role) {
        UUID tenantId = role == Role.SUPERADMIN ? null : UUID.randomUUID();
        return User.createActive(tenantId, "test@example.com", "encoded_pass", role,
                com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678", null, null, null);
    }

    @Test
    void login_happyPath_returnsLoginResult() {
        User user = activeUser(Role.ADMIN);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded_pass")).thenReturn(true);
        when(tokenGenerator.generateRawToken()).thenReturn("raw_refresh");
        when(tokenGenerator.hashToken("raw_refresh")).thenReturn("hashed_refresh");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginResult result = loginService.login(new LoginCommand("test@example.com", "password"));

        assertNotNull(result);
        assertEquals(user.getId(), result.userId());
        assertEquals(Role.ADMIN, result.primaryRole());
        assertEquals("/admin/dashboard", result.dashboardUrl());
        assertNotNull(result.accessToken());
        assertEquals("raw_refresh", result.refreshToken());
    }

    @Test
    void login_superadmin_returnsSuperadminDashboard() {
        User user = activeUser(Role.SUPERADMIN);
        when(userRepository.findByEmail("admin@klasio.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded_pass")).thenReturn(true);
        when(tokenGenerator.generateRawToken()).thenReturn("raw");
        when(tokenGenerator.hashToken("raw")).thenReturn("hashed");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginResult result = loginService.login(new LoginCommand("admin@klasio.local", "password"));
        assertEquals("/superadmin/dashboard", result.dashboardUrl());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = activeUser(Role.ADMIN);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded_pass")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(InvalidCredentialsException.class,
                () -> loginService.login(new LoginCommand("test@example.com", "wrong")));
    }

    @Test
    void login_wrongPassword_incrementsFailedCount() {
        User user = activeUser(Role.ADMIN);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded_pass")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(InvalidCredentialsException.class,
                () -> loginService.login(new LoginCommand("test@example.com", "wrong")));
        verify(userRepository).save(argThat(u -> u.getFailedLoginCount() == 1));
    }

    @Test
    void login_fifthFailure_locksAccount() {
        User user = new User(UUID.randomUUID(), UUID.randomUUID(), "test@example.com", "encoded_pass",
                Set.of(Role.ADMIN), UserStatus.ACTIVE, 4, null, Instant.now(), Instant.now(),
                com.klasio.shared.domain.model.IdentityDocumentType.CC, "10000001", null, null, null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded_pass")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(InvalidCredentialsException.class,
                () -> loginService.login(new LoginCommand("test@example.com", "wrong")));
        verify(userRepository).save(argThat(u -> u.isLocked()));
    }

    @Test
    void login_lockedAccount_throwsAccountLocked() {
        User user = new User(UUID.randomUUID(), UUID.randomUUID(), "test@example.com", "encoded_pass",
                Set.of(Role.ADMIN), UserStatus.ACTIVE, 5, Instant.now().plus(Duration.ofMinutes(15)),
                Instant.now(), Instant.now(),
                com.klasio.shared.domain.model.IdentityDocumentType.CC, "10000001", null, null, null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(AccountLockedException.class,
                () -> loginService.login(new LoginCommand("test@example.com", "password")));
    }

    @Test
    void login_pendingSetupAccount_throwsAccountSetupPending() {
        // Users who have not completed account setup have a null password hash
        User user = User.createPendingSetup(UUID.randomUUID(), "test@example.com", Role.STUDENT,
                com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678", null, null, null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(AccountSetupPendingException.class,
                () -> loginService.login(new LoginCommand("test@example.com", "password")));
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> loginService.login(new LoginCommand("nobody@example.com", "password")));
    }
}
