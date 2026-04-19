package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.AdminSummary;
import com.klasio.auth.application.dto.CreateAdminCommand;
import com.klasio.auth.application.port.PasswordEncoder;
import com.klasio.auth.application.port.TenantNamePort;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.exception.EmailAlreadyRegisteredException;
import com.klasio.auth.domain.exception.IdentityNumberAlreadyRegisteredException;
import com.klasio.auth.domain.exception.PasswordPolicyViolationException;
import com.klasio.auth.domain.model.PasswordPolicy;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.shared.domain.model.IdentityDocumentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CreateAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantNamePort tenantNamePort;

    public CreateAdminService(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              TenantNamePort tenantNamePort) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantNamePort = tenantNamePort;
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

        // 3. Validate password policy
        List<String> violations = PasswordPolicy.validate(command.password());
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }

        // 4. Parse document type
        IdentityDocumentType docType;
        try {
            docType = IdentityDocumentType.valueOf(command.identityDocumentType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid identity document type: " + command.identityDocumentType());
        }

        // 5. Create user — ACTIVE immediately (admin accounts don't require email verification)
        String passwordHash = passwordEncoder.encode(command.password());
        User admin = User.createActive(command.tenantId(), command.email(), passwordHash,
                Role.ADMIN, docType, command.identityNumber(),
                command.firstName(), command.lastName(), command.phoneNumber());
        userRepository.save(admin);

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
