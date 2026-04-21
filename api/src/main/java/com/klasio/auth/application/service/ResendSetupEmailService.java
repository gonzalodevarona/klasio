package com.klasio.auth.application.service;

import com.klasio.auth.application.port.AccountSetupTokenRepository;
import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.model.AccountSetupToken;
import com.klasio.auth.domain.model.UserStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class ResendSetupEmailService {

    private final UserRepository userRepository;
    private final AccountSetupTokenRepository accountSetupTokenRepository;
    private final TenantResolverPort tenantResolverPort;
    private final TokenGenerator tokenGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public ResendSetupEmailService(UserRepository userRepository,
                                   AccountSetupTokenRepository accountSetupTokenRepository,
                                   TenantResolverPort tenantResolverPort,
                                   TokenGenerator tokenGenerator,
                                   ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.accountSetupTokenRepository = accountSetupTokenRepository;
        this.tenantResolverPort = tenantResolverPort;
        this.tokenGenerator = tokenGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void resend(String email, String tenantSlug) {
        Optional<UUID> tenantIdOpt = tenantResolverPort.resolveTenantIdBySlug(tenantSlug);
        if (tenantIdOpt.isEmpty()) {
            // Silently return — do not expose whether the tenant exists
            return;
        }

        UUID tenantId = tenantIdOpt.get();

        Optional<com.klasio.auth.domain.model.User> userOpt =
                userRepository.findByEmailAndTenantId(email, tenantId);
        if (userOpt.isEmpty()) {
            // Silently return — do not expose whether the email exists
            return;
        }

        com.klasio.auth.domain.model.User user = userOpt.get();

        if (user.getStatus() == UserStatus.ACTIVE) {
            // Setup already completed — silently return
            return;
        }

        // Invalidate any existing setup tokens for this user
        accountSetupTokenRepository.invalidateAllByUserId(user.getId());

        // Generate a new 15-minute setup token
        String rawToken = tokenGenerator.generateRawToken();
        String hashedToken = tokenGenerator.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        AccountSetupToken newToken = AccountSetupToken.create(user.getId(), hashedToken, expiresAt);
        accountSetupTokenRepository.save(newToken);

        // TODO Task 3: publish AccountSetupInitiated event with rawToken so the email listener can send the email
    }
}
