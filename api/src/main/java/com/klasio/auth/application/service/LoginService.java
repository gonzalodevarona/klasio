package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.LoginCommand;
import com.klasio.auth.application.dto.LoginResult;
import com.klasio.auth.application.port.PasswordEncoder;
import com.klasio.auth.application.port.RefreshTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.UserAccountLockedEvent;
import com.klasio.auth.domain.event.UserLoggedInEvent;
import com.klasio.auth.domain.event.UserLoginFailedEvent;
import com.klasio.auth.domain.exception.InvalidCredentialsException;
import com.klasio.auth.domain.model.RefreshToken;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.infrastructure.config.AuthProperties;
import com.klasio.auth.infrastructure.security.JwtTokenService;
import com.klasio.shared.infrastructure.config.JwtProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenGenerator tokenGenerator;
    private final JwtTokenService jwtTokenService;
    private final AuthProperties authProperties;
    private final JwtProperties jwtProperties;
    private final ApplicationEventPublisher eventPublisher;

    public LoginService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        RefreshTokenRepository refreshTokenRepository,
                        TokenGenerator tokenGenerator,
                        JwtTokenService jwtTokenService,
                        AuthProperties authProperties,
                        JwtProperties jwtProperties,
                        ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.jwtTokenService = jwtTokenService;
        this.authProperties = authProperties;
        this.jwtProperties = jwtProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        user.validateCanLogin();

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            user.recordFailedLogin(authProperties.maxFailedLoginAttempts(),
                    Duration.ofMinutes(authProperties.lockoutDurationMinutes()));
            userRepository.save(user);

            eventPublisher.publishEvent(new UserLoginFailedEvent(
                    user.getId(), user.getTenantId(), user.getEmail(),
                    user.getFailedLoginCount(), Instant.now()));

            if (user.isLocked()) {
                eventPublisher.publishEvent(new UserAccountLockedEvent(
                        user.getId(), user.getTenantId(), user.getEmail(),
                        user.getLockedUntil(), Instant.now()));
            }

            throw new InvalidCredentialsException();
        }

        user.recordSuccessfulLogin();
        userRepository.save(user);

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(), user.getTenantId(), user.getRole());

        String rawRefreshToken = tokenGenerator.generateRawToken();
        String hashedRefreshToken = tokenGenerator.hashToken(rawRefreshToken);
        Instant refreshExpiresAt = Instant.now().plusMillis(jwtProperties.refreshTokenExpiration());

        RefreshToken refreshToken = RefreshToken.create(
                user.getId(), hashedRefreshToken, user.getTenantId(), refreshExpiresAt);
        refreshTokenRepository.save(refreshToken);

        eventPublisher.publishEvent(new UserLoggedInEvent(
                user.getId(), user.getTenantId(), user.getEmail(),
                user.getRole(), Instant.now()));

        return new LoginResult(
                user.getId(),
                user.getRole(),
                user.getTenantId(),
                user.getRole().dashboardUrl(),
                accessToken,
                rawRefreshToken
        );
    }
}
