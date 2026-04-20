package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.ProfessorUserIdPort;
import com.klasio.attendance.domain.port.StudentUserIdPort;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves users.id from either a student profile ID or a professor ID.
 *
 * For students: bridges via students.user_id (added in V032).
 * For professors: bridges via email — professors have no user_id FK yet (RF-32 pending),
 *   but both professors.email and users.email are unique identifiers shared between tables.
 *
 * Uses native SQL via EntityManager, following the same pattern as ProfessorIdLookupAdapter.
 */
@Slf4j
@Component
public class StudentAndProfessorUserIdAdapter implements StudentUserIdPort, ProfessorUserIdPort {

    private final EntityManager em;

    public StudentAndProfessorUserIdAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<UUID> findUserIdByStudentId(UUID tenantId, UUID studentId) {
        @SuppressWarnings("unchecked")
        List<UUID> userIds = em.createNativeQuery(
                        "SELECT user_id FROM students WHERE id = :studentId AND tenant_id = :tenantId")
                .setParameter("studentId", studentId)
                .setParameter("tenantId", tenantId)
                .getResultList();

        if (userIds.isEmpty() || userIds.get(0) == null) {
            log.debug("No user account linked for studentId={}", studentId);
            return Optional.empty();
        }
        return Optional.of(userIds.get(0));
    }

    @Override
    public Optional<UUID> findUserIdByProfessorId(UUID tenantId, UUID professorId) {
        // Step 1: resolve professor's email
        @SuppressWarnings("unchecked")
        List<String> emails = em.createNativeQuery(
                        "SELECT email FROM professors WHERE id = :professorId AND tenant_id = :tenantId")
                .setParameter("professorId", professorId)
                .setParameter("tenantId", tenantId)
                .getResultList();

        if (emails.isEmpty()) {
            log.debug("No professor found for professorId={}", professorId);
            return Optional.empty();
        }
        String email = emails.get(0);

        // Step 2: find user by email
        @SuppressWarnings("unchecked")
        List<UUID> userIds = em.createNativeQuery(
                        "SELECT id FROM users WHERE email = :email")
                .setParameter("email", email)
                .getResultList();

        if (userIds.isEmpty()) {
            log.debug("No user account found for professorId={} (email={})", professorId, email);
            return Optional.empty();
        }
        return Optional.of(userIds.get(0));
    }
}
