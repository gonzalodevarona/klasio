package com.klasio.auth.application.service;

import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.exception.ManagerNotFoundException;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DeactivateManagerService {

    private final UserRepository userRepository;

    public DeactivateManagerService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(UUID managerId, UUID scopeTenantId) {
        User manager = userRepository.findById(managerId)
                .filter(u -> u.hasRole(Role.MANAGER))
                .filter(u -> scopeTenantId == null || scopeTenantId.equals(u.getTenantId()))
                .orElseThrow(() -> new ManagerNotFoundException(
                        "Manager with id '%s' not found".formatted(managerId)));

        manager.deactivate();
        userRepository.save(manager);
    }
}
