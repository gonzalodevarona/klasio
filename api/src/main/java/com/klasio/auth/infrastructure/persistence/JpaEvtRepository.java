package com.klasio.auth.infrastructure.persistence;

import com.klasio.auth.application.port.EmailVerificationTokenRepository;
import com.klasio.auth.domain.model.EmailVerificationToken;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaEvtRepository implements EmailVerificationTokenRepository {

    private final SpringDataEvtRepository springDataRepo;

    public JpaEvtRepository(SpringDataEvtRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        EmailVerificationTokenJpaEntity entity = EmailVerificationTokenJpaEntity.fromDomain(token);
        return springDataRepo.save(entity).toDomain();
    }

    @Override
    public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
        return springDataRepo.findByTokenHash(tokenHash).map(EmailVerificationTokenJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void invalidateAllByUserId(UUID userId) {
        springDataRepo.invalidateAllByUserId(userId);
    }
}
