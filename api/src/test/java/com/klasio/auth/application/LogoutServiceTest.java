package com.klasio.auth.application;

import com.klasio.auth.application.port.RefreshTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.service.LogoutService;
import com.klasio.auth.domain.model.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;

    private LogoutService logoutService;

    @BeforeEach
    void setUp() {
        logoutService = new LogoutService(refreshTokenRepository, tokenGenerator, eventPublisher);
    }

    @Test
    void logout_validRefreshToken_revokesIt() {
        String rawToken = "raw_refresh_token";
        String hashedToken = "hashed_token";
        UUID userId = UUID.randomUUID();
        RefreshToken token = RefreshToken.create(userId, hashedToken, UUID.randomUUID(),
                Instant.now().plusSeconds(604800));

        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        logoutService.logout(rawToken, userId, null);

        verify(refreshTokenRepository).save(argThat(RefreshToken::isRevoked));
    }

    @Test
    void logout_tokenNotFound_handlesGracefully() {
        UUID userId = UUID.randomUUID();
        when(tokenGenerator.hashToken("unknown")).thenReturn("hashed_unknown");
        when(refreshTokenRepository.findByTokenHash("hashed_unknown")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> logoutService.logout("unknown", userId, null));
    }
}
