package com.klasio.auth.infrastructure.persistence;

import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository springDataRepo;

    public JpaUserRepository(SpringDataUserRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        return springDataRepo.save(entity).toDomain();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataRepo.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmailAndTenantId(String email, UUID tenantId) {
        return springDataRepo.findByEmailAndTenantId(email, tenantId).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataRepo.findByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmailAndTenantId(String email, UUID tenantId) {
        return springDataRepo.existsByEmailAndTenantId(email, tenantId);
    }

    @Override
    public boolean existsByIdentityNumberAndTenantId(UUID tenantId, String identityNumber) {
        return springDataRepo.existsByTenantIdAndIdentityNumber(tenantId, identityNumber);
    }

    @Override
    public Optional<User> findFirstByEmailAndStatus(String email, UserStatus status) {
        return springDataRepo.findFirstByEmailAndStatus(email, status).map(UserJpaEntity::toDomain);
    }

    @Override
    public Page<User> findByRole(Role role, UUID tenantId, UserStatus status, Pageable pageable) {
        return springDataRepo.findByRoleAndOptionalTenantAndStatus(role, tenantId, status, pageable)
                .map(UserJpaEntity::toDomain);
    }
}
