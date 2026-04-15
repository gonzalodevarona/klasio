package com.klasio.auth.application;

import com.klasio.auth.application.port.RefreshTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.application.service.RefreshTokenService;
import com.klasio.auth.domain.model.RefreshToken;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.infrastructure.security.JwtTokenService;
import com.klasio.shared.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private UserRepository userRepository;

    private RefreshTokenService refreshTokenService;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProps = new JwtProperties(
                "local-dev-secret-key-that-is-at-least-256-bits-long-for-hs256-signing",
                28800000L, 604800000L);
        jwtTokenService = new JwtTokenService(jwtProps);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, tokenGenerator,
                userRepository, jwtTokenService, jwtProps);
    }

    @Test
    void refresh_validToken_issuesNewTokenAndRevokesOld() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String oldHash = "old_hash";
        RefreshToken oldToken = new RefreshToken(UUID.randomUUID(), userId, oldHash, tenantId,
                Instant.now(), Instant.now().plusSeconds(604800), false, null);
        User user = User.createActive(tenantId, "test@example.com", "hash", Role.ADMIN, com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678");

        when(tokenGenerator.hashToken("old_raw")).thenReturn(oldHash);
        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(oldToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenGenerator.generateRawToken()).thenReturn("new_raw");
        when(tokenGenerator.hashToken("new_raw")).thenReturn("new_hash");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = refreshTokenService.refresh("old_raw");

        assertNotNull(result);
        assertEquals("new_raw", result.refreshToken());
        assertNotNull(result.accessToken());
        verify(refreshTokenRepository, times(2)).save(any());
    }

    @Test
    void refresh_expiredToken_throwsException() {
        String hash = "expired_hash";
        RefreshToken expired = new RefreshToken(UUID.randomUUID(), UUID.randomUUID(), hash,
                UUID.randomUUID(), Instant.now().minusSeconds(700000), Instant.now().minusSeconds(1),
                false, null);

        when(tokenGenerator.hashToken("expired_raw")).thenReturn(hash);
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(expired));

        assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.refresh("expired_raw"));
    }

    @Test
    void refresh_revokedToken_throwsAndRevokesChain() {
        UUID userId = UUID.randomUUID();
        String hash = "revoked_hash";
        RefreshToken revoked = new RefreshToken(UUID.randomUUID(), userId, hash,
                UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(604800),
                true, null);

        when(tokenGenerator.hashToken("revoked_raw")).thenReturn(hash);
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(revoked));

        assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.refresh("revoked_raw"));
        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }
}
