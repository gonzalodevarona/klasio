package com.klasio.auth.application.service;

import com.klasio.auth.application.port.PasswordEncoder;
import com.klasio.auth.application.port.PasswordResetTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.PasswordResetCompletedEvent;
import com.klasio.auth.domain.exception.PasswordPolicyViolationException;
import com.klasio.auth.domain.exception.ResetTokenAlreadyUsedException;
import com.klasio.auth.domain.exception.ResetTokenExpiredException;
import com.klasio.auth.domain.model.PasswordPolicy;
import com.klasio.auth.domain.model.PasswordResetToken;
import com.klasio.auth.domain.model.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ResetPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository prtRepository;
    private final TokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public ResetPasswordService(UserRepository userRepository,
                                PasswordResetTokenRepository prtRepository,
                                TokenGenerator tokenGenerator,
                                PasswordEncoder passwordEncoder,
                                ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.prtRepository = prtRepository;
        this.tokenGenerator = tokenGenerator;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        String hashedToken = tokenGenerator.hashToken(rawToken);
        PasswordResetToken token = prtRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (token.isUsed()) {
            throw new ResetTokenAlreadyUsedException();
        }

        if (token.isExpired()) {
            throw new ResetTokenExpiredException();
        }

        List<String> violations = PasswordPolicy.validate(newPassword);
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for token"));

        user.changePassword(passwordEncoder.encode(newPassword));
        token.markUsed();

        userRepository.save(user);
        prtRepository.save(token);

        eventPublisher.publishEvent(new PasswordResetCompletedEvent(
                user.getId(), user.getTenantId(), user.getEmail(), Instant.now()));
    }
}
