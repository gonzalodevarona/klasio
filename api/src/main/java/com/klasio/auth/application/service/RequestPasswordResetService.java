package com.klasio.auth.application.service;

import com.klasio.auth.application.port.*;
import com.klasio.auth.domain.event.PasswordResetRequestedEvent;
import com.klasio.auth.domain.model.PasswordResetToken;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.infrastructure.config.AuthProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class RequestPasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository prtRepository;
    private final TokenGenerator tokenGenerator;
    private final AuthProperties authProperties;
    private final ApplicationEventPublisher eventPublisher;

    public RequestPasswordResetService(UserRepository userRepository,
                                       PasswordResetTokenRepository prtRepository,
                                       TokenGenerator tokenGenerator,
                                       AuthProperties authProperties,
                                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.prtRepository = prtRepository;
        this.tokenGenerator = tokenGenerator;
        this.authProperties = authProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void requestReset(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        prtRepository.invalidateAllByUserId(user.getId());

        String rawToken = tokenGenerator.generateRawToken();
        String hashedToken = tokenGenerator.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(authProperties.passwordResetExpiryMinutes(), ChronoUnit.MINUTES);

        PasswordResetToken token = PasswordResetToken.create(user.getId(), hashedToken, expiresAt);
        prtRepository.save(token);

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                user.getId(), user.getTenantId(), email,
                rawToken, expiresAt,
                Instant.now()));
    }
}
