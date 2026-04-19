package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.AdminSummary;
import com.klasio.auth.application.dto.UpdateAdminCommand;
import com.klasio.auth.application.port.TenantNamePort;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.exception.AdminNotFoundException;
import com.klasio.auth.domain.exception.EmailAlreadyRegisteredException;
import com.klasio.auth.domain.exception.IdentityNumberAlreadyRegisteredException;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.shared.domain.model.IdentityDocumentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UpdateAdminService {

    private final UserRepository userRepository;
    private final TenantNamePort tenantNamePort;

    public UpdateAdminService(UserRepository userRepository, TenantNamePort tenantNamePort) {
        this.userRepository = userRepository;
        this.tenantNamePort = tenantNamePort;
    }

    public AdminSummary execute(UpdateAdminCommand command) {
        User admin = userRepository.findById(command.adminId())
                .filter(u -> u.hasRole(Role.ADMIN))
                .orElseThrow(() -> new AdminNotFoundException(
                        "Admin with id '%s' not found".formatted(command.adminId())));

        // Validate email uniqueness within the tenant (only if email is being changed)
        if (command.email() != null && !command.email().equalsIgnoreCase(admin.getEmail())) {
            if (userRepository.existsByEmailAndTenantId(command.email(), admin.getTenantId())) {
                throw new EmailAlreadyRegisteredException();
            }
        }

        // Validate identity number uniqueness within the tenant (only if changing)
        if (command.identityNumber() != null
                && !command.identityNumber().equals(admin.getIdentityNumber())) {
            if (userRepository.existsByIdentityNumberAndTenantId(
                    admin.getTenantId(), command.identityNumber())) {
                throw new IdentityNumberAlreadyRegisteredException();
            }
        }

        // Parse doc type if provided
        IdentityDocumentType docType = null;
        if (command.identityDocumentType() != null) {
            try {
                docType = IdentityDocumentType.valueOf(command.identityDocumentType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid identity document type: " + command.identityDocumentType());
            }
        }

        admin.updateProfile(command.firstName(), command.lastName(),
                command.email(), docType, command.identityNumber(), command.phoneNumber());
        userRepository.save(admin);

        Map<UUID, String> names = admin.getTenantId() != null
                ? tenantNamePort.findNamesByIds(Set.of(admin.getTenantId()))
                : Map.of();
        String tenantName = admin.getTenantId() != null
                ? names.getOrDefault(admin.getTenantId(), admin.getTenantId().toString())
                : null;

        return new AdminSummary(
                admin.getId(), admin.getTenantId(), tenantName,
                admin.getEmail(), admin.getFirstName(), admin.getLastName(),
                admin.getIdentityDocumentType().name(), admin.getIdentityNumber(),
                admin.getPhoneNumber(), admin.getStatus().name(), admin.getCreatedAt()
        );
    }
}
