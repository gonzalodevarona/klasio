package com.klasio.auth.application.service;

import com.klasio.auth.application.port.AccountSetupTokenRepository;
import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.AccountSetupInitiated;
import com.klasio.auth.domain.model.AccountSetupToken;
import com.klasio.auth.domain.model.User;
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

    /**
     * Resends the account-setup email.
     *
     * When {@code tenantSlug} is blank (e.g. the user reached the expired-link
     * screen without tenant context), we fall back to an email-only lookup across
     * all tenants and send to the first EMAIL_UNVERIFIED account found for that
     * address.  In practice a given email lives in a single tenant, so this is safe.
     */
    @Transactional
    public void resend(String email, String tenantSlug) {
        Optional<User> userOpt;

        if (tenantSlug == null || tenantSlug.isBlank()) {
            // No tenant context — look up by email + pending-setup status only
            userOpt = userRepository.findFirstByEmailAndStatus(email, UserStatus.EMAIL_UNVERIFIED);
        } else {
            Optional<UUID> tenantIdOpt = tenantResolverPort.resolveTenantIdBySlug(tenantSlug);
            if (tenantIdOpt.isEmpty()) {
                // Silently return — do not expose whether the tenant exists
                return;
            }
            userOpt = userRepository.findByEmailAndTenantId(email, tenantIdOpt.get());
        }

        if (userOpt.isEmpty()) {
            // Silently return — do not expose whether the email exists
            return;
        }

        User user = userOpt.get();

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

        eventPublisher.publishEvent(new AccountSetupInitiated(
                user.getId(), user.getTenantId(), user.getEmail(),
                user.getFirstName() != null ? user.getFirstName() + " " + user.getLastName() : user.getEmail(),
                user.primaryRole() != null ? user.primaryRole().name() : "USER",
                rawToken, expiresAt, Instant.now()));
    }
}
