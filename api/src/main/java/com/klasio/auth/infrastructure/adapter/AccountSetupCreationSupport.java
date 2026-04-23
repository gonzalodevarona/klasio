package com.klasio.auth.infrastructure.adapter;

import com.klasio.auth.application.port.AccountSetupTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.AccountSetupInitiated;
import com.klasio.auth.domain.exception.EmailAlreadyRegisteredException;
import com.klasio.auth.domain.model.AccountSetupToken;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.shared.domain.model.IdentityDocumentType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Package-private helper that contains the shared account-setup creation logic
 * used by both {@link StudentAccountSetupAdapter} and {@link ProfessorAccountSetupAdapter}.
 * Centralising here avoids duplicating the email-collision guard, token lifecycle,
 * and event dispatch across every role-specific adapter.
 */
@Component
class AccountSetupCreationSupport {

    private final UserRepository userRepository;
    private final AccountSetupTokenRepository accountSetupTokenRepository;
    private final TokenGenerator tokenGenerator;
    private final ApplicationEventPublisher eventPublisher;

    AccountSetupCreationSupport(UserRepository userRepository,
                                AccountSetupTokenRepository accountSetupTokenRepository,
                                TokenGenerator tokenGenerator,
                                ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.accountSetupTokenRepository = accountSetupTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a {@link User} in {@code INVITED} state, persists a 15-minute
     * {@link AccountSetupToken}, and publishes an {@link AccountSetupInitiated} event
     * so the user receives a one-time setup link by email.
     *
     * @param tenantId       the tenant the user belongs to
     * @param email          the user's email address
     * @param firstName      first name
     * @param lastName       last name
     * @param docType        identity document type (required for all roles that have identity data)
     * @param identityNumber identity document number (required when docType is provided)
     * @param phone          optional phone number
     * @param role           the role to assign to the new user
     * @param roleName       lowercase role label included in the setup email (e.g. "student", "professor")
     * @return the newly created user's UUID
     * @throws EmailAlreadyRegisteredException if the email is already registered for this tenant
     */
    UUID createAndDispatch(UUID tenantId, String email, String firstName, String lastName,
            IdentityDocumentType docType, String identityNumber, String phone,
            Role role, String roleName) {
        if (userRepository.existsByEmailAndTenantId(email, tenantId)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = User.createPendingSetup(tenantId, email, role, docType, identityNumber,
                firstName, lastName, phone);
        userRepository.save(user);

        String rawToken = tokenGenerator.generateRawToken();
        String hashedToken = tokenGenerator.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        AccountSetupToken token = AccountSetupToken.create(user.getId(), hashedToken, expiresAt);
        accountSetupTokenRepository.save(token);

        eventPublisher.publishEvent(new AccountSetupInitiated(
                user.getId(), tenantId, email, firstName + " " + lastName,
                roleName, rawToken, expiresAt, Instant.now()));

        return user.getId();
    }
}
