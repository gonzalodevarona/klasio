package com.klasio.auth.application.service;

import com.klasio.auth.application.port.EmailVerificationTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.VerificationEmailResendRequested;
import com.klasio.auth.domain.model.EmailVerificationToken;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import com.klasio.auth.infrastructure.config.AuthProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class ResendVerificationEmailService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository evtRepository;
    private final TokenGenerator tokenGenerator;
    private final AuthProperties authProperties;
    private final ApplicationEventPublisher eventPublisher;

    public ResendVerificationEmailService(UserRepository userRepository,
                                          EmailVerificationTokenRepository evtRepository,
                                          TokenGenerator tokenGenerator,
                                          AuthProperties authProperties,
                                          ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.evtRepository = evtRepository;
        this.tokenGenerator = tokenGenerator;
        this.authProperties = authProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void resend(String email, String tenantSlug) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        if (user.getStatus() != UserStatus.EMAIL_UNVERIFIED) {
            return;
        }

        evtRepository.invalidateAllByUserId(user.getId());

        String rawToken = tokenGenerator.generateRawToken();
        String hashedToken = tokenGenerator.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(authProperties.emailVerificationExpiryHours(), ChronoUnit.HOURS);

        EmailVerificationToken token = EmailVerificationToken.create(user.getId(), hashedToken, expiresAt);
        evtRepository.save(token);

        eventPublisher.publishEvent(new VerificationEmailResendRequested(
                user.getId(), user.getTenantId(), email, tenantSlug,
                rawToken, expiresAt,
                Instant.now()));
    }
}
