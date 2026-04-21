package com.klasio.auth.infrastructure.adapter;

import com.klasio.auth.application.port.AccountSetupTokenRepository;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.AccountSetupInitiated;
import com.klasio.auth.domain.model.AccountSetupToken;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.student.domain.port.AccountSetupCreationPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Implements {@link AccountSetupCreationPort} for the student module.
 * Lives in the auth module because it orchestrates User creation and
 * AccountSetupToken generation — both auth concerns.
 * Must run inside an existing transaction (MANDATORY) to ensure atomicity
 * with the student record created by the caller.
 */
@Component
public class StudentAccountSetupAdapter implements AccountSetupCreationPort {

    private final UserRepository userRepository;
    private final AccountSetupTokenRepository accountSetupTokenRepository;
    private final TokenGenerator tokenGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public StudentAccountSetupAdapter(UserRepository userRepository,
                                      AccountSetupTokenRepository accountSetupTokenRepository,
                                      TokenGenerator tokenGenerator,
                                      ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.accountSetupTokenRepository = accountSetupTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID createAndDispatchSetup(UUID tenantId, String email, String firstName, String lastName,
            IdentityDocumentType docType, String identityNumber, String phone, UUID studentId) {
        User user = User.createPendingSetup(tenantId, email, Role.STUDENT,
                docType, identityNumber, firstName, lastName, phone);
        userRepository.save(user);

        String rawToken = tokenGenerator.generateRawToken();
        String hashedToken = tokenGenerator.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        AccountSetupToken token = AccountSetupToken.create(user.getId(), hashedToken, expiresAt);
        accountSetupTokenRepository.save(token);

        eventPublisher.publishEvent(new AccountSetupInitiated(
                user.getId(), tenantId, email, firstName + " " + lastName,
                "student", rawToken, expiresAt, Instant.now()));

        return user.getId();
    }
}
