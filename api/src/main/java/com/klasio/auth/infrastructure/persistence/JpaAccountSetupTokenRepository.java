package com.klasio.auth.infrastructure.persistence;

import com.klasio.auth.domain.model.AccountSetupToken;
import com.klasio.auth.domain.port.AccountSetupTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAccountSetupTokenRepository implements AccountSetupTokenRepository {

    private final SpringDataAccountSetupTokenRepository springDataRepo;

    public JpaAccountSetupTokenRepository(SpringDataAccountSetupTokenRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public void save(AccountSetupToken token) {
        springDataRepo.save(AccountSetupTokenJpaEntity.fromDomain(token));
    }

    @Override
    public Optional<AccountSetupToken> findByTokenHash(String tokenHash) {
        return springDataRepo.findByTokenHash(tokenHash)
                .map(AccountSetupTokenJpaEntity::toDomain);
    }

    @Override
    public void invalidateAllForUser(UUID userId) {
        var tokens = springDataRepo.findAllByUserId(userId);
        tokens.forEach(t -> t.setUsed(true));
        springDataRepo.saveAll(tokens);
    }
}
