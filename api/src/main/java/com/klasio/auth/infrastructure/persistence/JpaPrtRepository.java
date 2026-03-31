package com.klasio.auth.infrastructure.persistence;

import com.klasio.auth.application.port.PasswordResetTokenRepository;
import com.klasio.auth.domain.model.PasswordResetToken;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaPrtRepository implements PasswordResetTokenRepository {

    private final SpringDataPrtRepository springDataRepo;

    public JpaPrtRepository(SpringDataPrtRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenJpaEntity entity = PasswordResetTokenJpaEntity.fromDomain(token);
        return springDataRepo.save(entity).toDomain();
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return springDataRepo.findByTokenHash(tokenHash).map(PasswordResetTokenJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void invalidateAllByUserId(UUID userId) {
        springDataRepo.invalidateAllByUserId(userId);
    }
}
