package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.RegisterStudentCommand;
import com.klasio.auth.application.port.AccountSetupTokenRepository;
import com.klasio.auth.application.port.StudentProfilePort;
import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.AccountSetupInitiated;
import com.klasio.auth.domain.exception.EmailAlreadyRegisteredException;
import com.klasio.auth.domain.exception.IdentityNumberAlreadyRegisteredException;
import com.klasio.auth.domain.model.AccountSetupToken;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.shared.domain.model.IdentityDocumentType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RegisterStudentService {

    private final UserRepository userRepository;
    private final StudentProfilePort studentProfilePort;
    private final TenantResolverPort tenantResolverPort;
    private final TokenGenerator tokenGenerator;
    private final AccountSetupTokenRepository accountSetupTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RegisterStudentService(UserRepository userRepository,
                                  StudentProfilePort studentProfilePort,
                                  TenantResolverPort tenantResolverPort,
                                  TokenGenerator tokenGenerator,
                                  AccountSetupTokenRepository accountSetupTokenRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.studentProfilePort = studentProfilePort;
        this.tenantResolverPort = tenantResolverPort;
        this.tokenGenerator = tokenGenerator;
        this.accountSetupTokenRepository = accountSetupTokenRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void register(RegisterStudentCommand command) {
        UUID tenantId = tenantResolverPort.resolveTenantIdBySlug(command.tenantSlug())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tenant with slug '%s' not found".formatted(command.tenantSlug())));

        if (userRepository.existsByEmailAndTenantId(command.email(), tenantId)) {
            throw new EmailAlreadyRegisteredException();
        }

        IdentityDocumentType docType = parseDocumentType(command.identityDocumentType());
        String identityNumber = command.identityNumber();

        if (userRepository.existsByIdentityNumberAndTenantId(tenantId, identityNumber)) {
            throw new IdentityNumberAlreadyRegisteredException();
        }

        if (studentProfilePort.existsByIdentityNumberInTenant(tenantId, identityNumber)) {
            throw new IdentityNumberAlreadyRegisteredException();
        }

        // Create user with no password — they will set it by clicking the account setup link.
        User user = User.createPendingSetup(tenantId, command.email(), Role.STUDENT,
                docType, identityNumber,
                command.firstName(), command.lastName(), null);
        userRepository.save(user);

        studentProfilePort.createStudentProfile(
                tenantId, command.firstName(), command.lastName(), command.email(),
                command.dateOfBirth(), command.identityDocumentType(), command.identityNumber(),
                command.eps(), command.tutorFullName(), command.tutorRelationship(),
                command.tutorContact(), user.getId());

        // Generate 15-minute account setup token and persist it.
        String rawToken = tokenGenerator.generateRawToken();
        String hashedToken = tokenGenerator.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        AccountSetupToken token = AccountSetupToken.create(user.getId(), hashedToken, expiresAt);
        accountSetupTokenRepository.save(token);

        eventPublisher.publishEvent(new AccountSetupInitiated(
                user.getId(), tenantId, command.email(),
                command.firstName() + " " + command.lastName(),
                "student", rawToken, expiresAt, Instant.now()));
    }

    private IdentityDocumentType parseDocumentType(String value) {
        try {
            return IdentityDocumentType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid identity document type: " + value);
        }
    }
}
