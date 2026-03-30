package com.klasio.auth.application.service;

import com.klasio.auth.application.port.EmailVerificationTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.EmailVerifiedEvent;
import com.klasio.auth.domain.exception.VerificationTokenAlreadyUsedException;
import com.klasio.auth.domain.exception.VerificationTokenExpiredException;
import com.klasio.auth.domain.model.EmailVerificationToken;
import com.klasio.auth.domain.model.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class VerifyEmailService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository evtRepository;
    private final TokenGenerator tokenGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public VerifyEmailService(UserRepository userRepository,
                              EmailVerificationTokenRepository evtRepository,
                              TokenGenerator tokenGenerator,
                              ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.evtRepository = evtRepository;
        this.tokenGenerator = tokenGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void verify(String rawToken) {
        String hashedToken = tokenGenerator.hashToken(rawToken);
        EmailVerificationToken token = evtRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (token.isUsed()) {
            throw new VerificationTokenAlreadyUsedException();
        }

        if (token.isExpired()) {
            throw new VerificationTokenExpiredException();
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for token"));

        user.verifyEmail();
        token.markUsed();

        userRepository.save(user);
        evtRepository.save(token);

        eventPublisher.publishEvent(new EmailVerifiedEvent(
                user.getId(), user.getTenantId(), user.getEmail(), Instant.now()));
    }
}
