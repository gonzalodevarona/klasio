package com.klasio.auth.application.service;

import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.exception.AdminNotFoundException;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DeactivateAdminService {

    private final UserRepository userRepository;

    public DeactivateAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(UUID adminId) {
        User admin = userRepository.findById(adminId)
                .filter(u -> u.hasRole(Role.ADMIN))
                .orElseThrow(() -> new AdminNotFoundException(
                        "Admin with id '%s' not found".formatted(adminId)));

        admin.deactivate();
        userRepository.save(admin);
    }
}
