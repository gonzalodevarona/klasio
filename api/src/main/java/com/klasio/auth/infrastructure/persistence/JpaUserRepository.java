package com.klasio.auth.infrastructure.persistence;

import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.model.User;
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
}
