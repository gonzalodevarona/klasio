package com.klasio.auth.application.port;

import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<User> findByEmail(String email);

    /**
     * Finds a user by email and status across all tenants.
     * Used when tenant context is unavailable (e.g. email-only resend-setup flow).
     * Returns the first match found — in practice a given email is used in one tenant only.
     */
    Optional<User> findFirstByEmailAndStatus(String email, UserStatus status);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByIdentityNumberAndTenantId(UUID tenantId, String identityNumber);

    /**
     * Paginated list of users with a given role, optionally filtered by tenant and status.
     * Used by SUPERADMIN to list all admins across all tenants.
     */
    Page<User> findByRole(Role role, UUID tenantId, UserStatus status, Pageable pageable);

    /**
     * Returns all users that belong to {@code tenantId} and whose id is in {@code userIds}.
     * IDs not found in the tenant are silently omitted.
     */
    List<User> findAllByIds(UUID tenantId, Set<UUID> userIds);
}
