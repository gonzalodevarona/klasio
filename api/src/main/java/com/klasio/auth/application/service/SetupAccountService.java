package com.klasio.auth.application.service;

import com.klasio.auth.application.port.AccountSetupTokenRepository;
import com.klasio.auth.application.port.PasswordEncoder;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.AccountSetupCompletedEvent;
import com.klasio.auth.domain.exception.AccountSetupTokenAlreadyUsedException;
import com.klasio.auth.domain.exception.AccountSetupTokenExpiredException;
import com.klasio.auth.domain.exception.AccountSetupTokenInvalidException;
import com.klasio.auth.domain.exception.PasswordPolicyViolationException;
import com.klasio.auth.domain.model.AccountSetupToken;
import com.klasio.auth.domain.model.PasswordPolicy;
import com.klasio.auth.domain.model.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SetupAccountService {

    private final UserRepository userRepository;
    private final AccountSetupTokenRepository accountSetupTokenRepository;
    private final TokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public SetupAccountService(UserRepository userRepository,
                               AccountSetupTokenRepository accountSetupTokenRepository,
                               TokenGenerator tokenGenerator,
                               PasswordEncoder passwordEncoder,
                               ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.accountSetupTokenRepository = accountSetupTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void setup(String rawToken, String newPassword) {
        String hashedToken = tokenGenerator.hashToken(rawToken);

        AccountSetupToken token = accountSetupTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(AccountSetupTokenInvalidException::new);

        if (token.isUsed()) {
            throw new AccountSetupTokenAlreadyUsedException();
        }

        if (token.isExpired()) {
            throw new AccountSetupTokenExpiredException();
        }

        List<String> violations = PasswordPolicy.validate(newPassword);
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for account setup token"));

        user.setupAccount(passwordEncoder.encode(newPassword));
        token.markUsed();

        userRepository.save(user);
        accountSetupTokenRepository.save(token);

        eventPublisher.publishEvent(new AccountSetupCompletedEvent(
                user.getId(), user.getTenantId(), user.getEmail(), Instant.now()));
    }
}
