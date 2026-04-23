package com.klasio.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAccountSetupTokenRepository
        extends JpaRepository<AccountSetupTokenJpaEntity, UUID> {

    Optional<AccountSetupTokenJpaEntity> findByTokenHash(String tokenHash);

    List<AccountSetupTokenJpaEntity> findAllByUserId(UUID userId);
}
