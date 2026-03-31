package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.AssignRoleCommand;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.RoleAssignedEvent;
import com.klasio.auth.domain.exception.RoleElevationForbiddenException;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AssignRoleService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AssignRoleService(UserRepository userRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void assign(AssignRoleCommand command) {
        User targetUser = userRepository.findById(command.targetUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Only SUPERADMIN and ADMIN can assign roles
        if (command.assignerRole() != Role.SUPERADMIN && command.assignerRole() != Role.ADMIN) {
            throw new RoleElevationForbiddenException(
                    "Role %s is not authorized to assign roles".formatted(command.assignerRole()));
        }

        // Hierarchy guard: assigner's role must be strictly above the target role
        if (!command.assignerRole().isAbove(command.newRole())) {
            throw new RoleElevationForbiddenException(
                    "Cannot assign role %s — your role %s is not above it"
                            .formatted(command.newRole(), command.assignerRole()));
        }

        // Tenant scope guard: non-SUPERADMIN can only assign within own tenant
        if (command.assignerRole() != Role.SUPERADMIN) {
            if (command.assignerTenantId() == null ||
                    !command.assignerTenantId().equals(targetUser.getTenantId())) {
                throw new RoleElevationForbiddenException(
                        "Cannot assign roles to users in a different tenant");
            }
        }

        Role previousRole = targetUser.getRole();
        targetUser.assignRole(command.newRole());
        userRepository.save(targetUser);

        eventPublisher.publishEvent(new RoleAssignedEvent(
                targetUser.getId(), targetUser.getTenantId(),
                previousRole, command.newRole(),
                command.assignerId(), Instant.now()));
    }
}
