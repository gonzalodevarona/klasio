package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.LoginResult;
import com.klasio.auth.application.port.RefreshTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.model.RefreshToken;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.infrastructure.security.JwtTokenService;
import com.klasio.shared.infrastructure.config.JwtProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenGenerator tokenGenerator;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                                TokenGenerator tokenGenerator,
                                UserRepository userRepository,
                                JwtTokenService jwtTokenService,
                                JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        String hash = tokenGenerator.hashToken(rawRefreshToken);
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (oldToken.isRevoked()) {
            // Stolen token detection: revoke entire chain
            refreshTokenRepository.revokeAllByUserId(oldToken.getUserId());
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (oldToken.isExpired()) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        User user = userRepository.findById(oldToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate new tokens
        String newRawRefreshToken = tokenGenerator.generateRawToken();
        String newHashedRefreshToken = tokenGenerator.hashToken(newRawRefreshToken);
        Instant newExpiresAt = Instant.now().plusMillis(jwtProperties.refreshTokenExpiration());

        RefreshToken newToken = RefreshToken.create(
                user.getId(), newHashedRefreshToken, user.getTenantId(), newExpiresAt);
        refreshTokenRepository.save(newToken);

        // Revoke old and set replacement pointer
        oldToken.replaceWith(newToken.getId());
        refreshTokenRepository.save(oldToken);

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(), user.getTenantId(), user.getRole());

        return new LoginResult(
                user.getId(),
                user.getRole(),
                user.getTenantId(),
                user.getRole().dashboardUrl(),
                accessToken,
                newRawRefreshToken
        );
    }
}
