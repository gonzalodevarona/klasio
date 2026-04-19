package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves professorId from a userId by bridging via email:
 * 1. Look up email from users table by userId.
 * 2. Look up professor by tenant + email.
 *
 * Professors do not yet have a user_id FK column (RF-32 email invite pending),
 * so email is the natural join key since it is unique per tenant in both tables.
 *
 * Implements both the attendance and programclass module ports — same contract, kept separate
 * to preserve hexagonal module boundaries.
 */
@Slf4j
@Component
public class ProfessorIdLookupAdapter
        implements ProfessorIdLookupPort,
                   com.klasio.programclass.domain.port.ProfessorIdLookupPort {

    private final EntityManager em;

    public ProfessorIdLookupAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<UUID> findProfessorIdByUserId(UUID tenantId, UUID userId) {
        // Step 1: resolve email from users table
        @SuppressWarnings("unchecked")
        List<String> emails = em.createNativeQuery(
                        "SELECT email FROM users WHERE id = :userId")
                .setParameter("userId", userId)
                .getResultList();

        if (emails.isEmpty()) {
            log.debug("No user found for userId={}", userId);
            return Optional.empty();
        }
        String email = emails.get(0);

        // Step 2: find professor by tenant + email
        @SuppressWarnings("unchecked")
        List<UUID> ids = em.createNativeQuery(
                        "SELECT id FROM professors WHERE tenant_id = :tenantId AND email = :email")
                .setParameter("tenantId", tenantId)
                .setParameter("email", email)
                .getResultList();

        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }
}
