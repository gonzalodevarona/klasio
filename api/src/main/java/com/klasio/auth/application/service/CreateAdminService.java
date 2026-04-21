package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.AdminSummary;
import com.klasio.auth.application.dto.CreateAdminCommand;
import com.klasio.auth.application.port.AccountSetupTokenRepository;
import com.klasio.auth.application.port.TenantNamePort;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CreateAdminService {

    private final UserRepository userRepository;
    private final TenantNamePort tenantNamePort;
    private final AccountSetupTokenRepository accountSetupTokenRepository;
    private final TokenGenerator tokenGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public CreateAdminService(UserRepository userRepository,
                              TenantNamePort tenantNamePort,
                              AccountSetupTokenRepository accountSetupTokenRepository,
                              TokenGenerator tokenGenerator,
                              ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.tenantNamePort = tenantNamePort;
        this.accountSetupTokenRepository = accountSetupTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.eventPublisher = eventPublisher;
    }

    public AdminSummary execute(CreateAdminCommand command) {
        // 1. Validate email uniqueness within the tenant
        if (userRepository.existsByEmailAndTenantId(command.email(), command.tenantId())) {
            throw new EmailAlreadyRegisteredException();
        }

        // 2. Validate identity number uniqueness within the tenant
        if (userRepository.existsByIdentityNumberAndTenantId(command.tenantId(), command.identityNumber())) {
            throw new IdentityNumberAlreadyRegisteredException();
        }

        // 3. Parse document type
        IdentityDocumentType docType;
        try {
            docType = IdentityDocumentType.valueOf(command.identityDocumentType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid identity document type: " + command.identityDocumentType());
        }

        // 4. Create user with no password — they will set it by clicking the account setup link.
        User admin = User.createPendingSetup(command.tenantId(), command.email(), Role.ADMIN,
                docType, command.identityNumber(),
                command.firstName(), command.lastName(), command.phoneNumber());
        userRepository.save(admin);

        // 5. Generate 15-minute account setup token and persist it.
        String rawToken = tokenGenerator.generateRawToken();
        String hashedToken = tokenGenerator.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        AccountSetupToken token = AccountSetupToken.create(admin.getId(), hashedToken, expiresAt);
        accountSetupTokenRepository.save(token);

        eventPublisher.publishEvent(new AccountSetupInitiated(
                admin.getId(), command.tenantId(), command.email(),
                command.firstName() + " " + command.lastName(),
                "admin", rawToken, expiresAt, Instant.now()));

        // 6. Resolve tenant name for the response
        Map<UUID, String> names = tenantNamePort.findNamesByIds(Set.of(command.tenantId()));
        String tenantName = names.getOrDefault(command.tenantId(), command.tenantId().toString());

        return new AdminSummary(
                admin.getId(),
                admin.getTenantId(),
                tenantName,
                admin.getEmail(),
                admin.getFirstName(),
                admin.getLastName(),
                admin.getIdentityDocumentType().name(),
                admin.getIdentityNumber(),
                admin.getPhoneNumber(),
                admin.getStatus().name(),
                admin.getCreatedAt()
        );
    }
}
