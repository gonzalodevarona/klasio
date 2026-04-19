package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.AdminSummary;
import com.klasio.auth.application.dto.UpdateAdminCommand;
import com.klasio.auth.application.port.TenantNamePort;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.exception.EmailAlreadyRegisteredException;
import com.klasio.auth.domain.exception.IdentityNumberAlreadyRegisteredException;
import com.klasio.auth.domain.exception.ManagerNotFoundException;
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
public class UpdateManagerService {

    private final UserRepository userRepository;
    private final TenantNamePort tenantNamePort;

    public UpdateManagerService(UserRepository userRepository, TenantNamePort tenantNamePort) {
        this.userRepository = userRepository;
        this.tenantNamePort = tenantNamePort;
    }

    public AdminSummary execute(UpdateAdminCommand command, UUID scopeTenantId) {
        User manager = userRepository.findById(command.adminId())
                .filter(u -> u.hasRole(Role.MANAGER))
                .filter(u -> scopeTenantId == null || scopeTenantId.equals(u.getTenantId()))
                .orElseThrow(() -> new ManagerNotFoundException(
                        "Manager with id '%s' not found".formatted(command.adminId())));

        if (command.email() != null && !command.email().equalsIgnoreCase(manager.getEmail())) {
            if (userRepository.existsByEmailAndTenantId(command.email(), manager.getTenantId())) {
                throw new EmailAlreadyRegisteredException();
            }
        }

        if (command.identityNumber() != null
                && !command.identityNumber().equals(manager.getIdentityNumber())) {
            if (userRepository.existsByIdentityNumberAndTenantId(
                    manager.getTenantId(), command.identityNumber())) {
                throw new IdentityNumberAlreadyRegisteredException();
            }
        }

        IdentityDocumentType docType = null;
        if (command.identityDocumentType() != null) {
            try {
                docType = IdentityDocumentType.valueOf(command.identityDocumentType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid identity document type: " + command.identityDocumentType());
            }
        }

        manager.updateProfile(command.firstName(), command.lastName(),
                command.email(), docType, command.identityNumber(), command.phoneNumber());
        userRepository.save(manager);

        Map<UUID, String> names = manager.getTenantId() != null
                ? tenantNamePort.findNamesByIds(Set.of(manager.getTenantId()))
                : Map.of();
        String tenantName = manager.getTenantId() != null
                ? names.getOrDefault(manager.getTenantId(), manager.getTenantId().toString())
                : null;

        return new AdminSummary(
                manager.getId(), manager.getTenantId(), tenantName,
                manager.getEmail(), manager.getFirstName(), manager.getLastName(),
                manager.getIdentityDocumentType().name(), manager.getIdentityNumber(),
                manager.getPhoneNumber(), manager.getStatus().name(), manager.getCreatedAt()
        );
    }
}
