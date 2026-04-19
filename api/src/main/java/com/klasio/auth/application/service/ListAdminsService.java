package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.AdminSummary;
import com.klasio.auth.application.port.TenantNamePort;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListAdminsService {

    private final UserRepository userRepository;
    private final TenantNamePort tenantNamePort;

    public ListAdminsService(UserRepository userRepository, TenantNamePort tenantNamePort) {
        this.userRepository = userRepository;
        this.tenantNamePort = tenantNamePort;
    }

    public Page<AdminSummary> execute(UUID tenantId, UserStatus status, Pageable pageable) {
        Page<User> userPage = userRepository.findByRole(Role.ADMIN, tenantId, status, pageable);

        // Bulk resolve tenant names so we don't N+1 per row
        Set<UUID> tenantIds = userPage.getContent().stream()
                .map(User::getTenantId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<UUID, String> tenantNames = tenantNamePort.findNamesByIds(tenantIds);

        return userPage.map(u -> new AdminSummary(
                u.getId(),
                u.getTenantId(),
                u.getTenantId() != null
                        ? tenantNames.getOrDefault(u.getTenantId(), u.getTenantId().toString())
                        : null,
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getIdentityDocumentType().name(),
                u.getIdentityNumber(),
                u.getPhoneNumber(),
                u.getStatus().name(),
                u.getCreatedAt()
        ));
    }
}
