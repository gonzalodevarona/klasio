package com.klasio.auth.infrastructure.persistence;

import com.klasio.auth.application.port.RefreshTokenRepository;
import com.klasio.auth.domain.model.RefreshToken;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository springDataRepo;

    public JpaRefreshTokenRepository(SpringDataRefreshTokenRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.fromDomain(token);
        return springDataRepo.save(entity).toDomain();
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return springDataRepo.findByTokenHash(tokenHash).map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId) {
        springDataRepo.revokeAllByUserId(userId);
    }
}
