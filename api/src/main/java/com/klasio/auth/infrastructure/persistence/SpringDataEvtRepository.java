package com.klasio.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataEvtRepository extends JpaRepository<EmailVerificationTokenJpaEntity, UUID> {

    Optional<EmailVerificationTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE EmailVerificationTokenJpaEntity e SET e.used = true WHERE e.userId = :userId AND e.used = false")
    void invalidateAllByUserId(UUID userId);
}
