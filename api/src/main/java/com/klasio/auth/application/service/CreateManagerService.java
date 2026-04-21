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
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.port.ProfessorRepository;
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
public class CreateManagerService {

    private final UserRepository userRepository;
    private final TenantNamePort tenantNamePort;
    private final ProfessorRepository professorRepository;
    private final AccountSetupTokenRepository accountSetupTokenRepository;
    private final TokenGenerator tokenGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public CreateManagerService(UserRepository userRepository,
                                TenantNamePort tenantNamePort,
                                ProfessorRepository professorRepository,
                                AccountSetupTokenRepository accountSetupTokenRepository,
                                TokenGenerator tokenGenerator,
                                ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.tenantNamePort = tenantNamePort;
        this.professorRepository = professorRepository;
        this.accountSetupTokenRepository = accountSetupTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.eventPublisher = eventPublisher;
    }

    public AdminSummary execute(CreateAdminCommand command) {
        if (userRepository.existsByEmailAndTenantId(command.email(), command.tenantId())) {
            throw new EmailAlreadyRegisteredException();
        }

        if (userRepository.existsByIdentityNumberAndTenantId(command.tenantId(), command.identityNumber())) {
            throw new IdentityNumberAlreadyRegisteredException();
        }

        IdentityDocumentType docType;
        try {
            docType = IdentityDocumentType.valueOf(command.identityDocumentType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid identity document type: " + command.identityDocumentType());
        }

        // Create user with no password — they will set it by clicking the account setup link.
        User manager = User.createPendingSetup(command.tenantId(), command.email(), Role.MANAGER,
                docType, command.identityNumber(),
                command.firstName(), command.lastName(), command.phoneNumber());
        userRepository.save(manager);

        // A Manager is also a Professor. Create the professor record so the manager appears in
        // professor lists and can be assigned to classes. Status is ACTIVE because the manager
        // already has a user record (no invitation flow for the professor side).
        Professor professor = Professor.createForManager(
                manager.getId(), command.tenantId(),
                manager.getFirstName(), manager.getLastName(), manager.getEmail(),
                manager.getPhoneNumber(), docType, command.identityNumber(), command.createdBy());
        professorRepository.save(professor);

        // Generate 15-minute account setup token and persist it.
        String rawToken = tokenGenerator.generateRawToken();
        String hashedToken = tokenGenerator.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        AccountSetupToken token = AccountSetupToken.create(manager.getId(), hashedToken, expiresAt);
        accountSetupTokenRepository.save(token);

        eventPublisher.publishEvent(new AccountSetupInitiated(
                manager.getId(), command.tenantId(), command.email(),
                command.firstName() + " " + command.lastName(),
                "manager", rawToken, expiresAt, Instant.now()));

        Map<UUID, String> names = tenantNamePort.findNamesByIds(Set.of(command.tenantId()));
        String tenantName = names.getOrDefault(command.tenantId(), command.tenantId().toString());

        return new AdminSummary(
                manager.getId(), manager.getTenantId(), tenantName,
                manager.getEmail(), manager.getFirstName(), manager.getLastName(),
                manager.getIdentityDocumentType().name(), manager.getIdentityNumber(),
                manager.getPhoneNumber(), manager.getStatus().name(), manager.getCreatedAt()
        );
    }
}
