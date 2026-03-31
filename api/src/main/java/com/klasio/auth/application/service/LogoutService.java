package com.klasio.auth.application.service;

import com.klasio.auth.application.port.RefreshTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.domain.event.UserLoggedOutEvent;
import com.klasio.auth.domain.model.RefreshToken;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenGenerator tokenGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public LogoutService(RefreshTokenRepository refreshTokenRepository,
                         TokenGenerator tokenGenerator,
                         ApplicationEventPublisher eventPublisher) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void logout(String rawRefreshToken, UUID userId, UUID tenantId) {
        if (rawRefreshToken != null) {
            String hash = tokenGenerator.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
            });
        }

        eventPublisher.publishEvent(new UserLoggedOutEvent(userId, tenantId, Instant.now()));
    }
}
