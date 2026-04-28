package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.ProfessorUserIdPort;
import com.klasio.attendance.domain.port.StudentUserIdPort;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
 * Extends TenantScopedRepository so that applyTenantContext() sets app.current_tenant on
 * the current connection before each query — required because the students and professors
 * tables have FORCE ROW LEVEL SECURITY, and this adapter is also called from
 * @TransactionalEventListener(AFTER_COMMIT) handlers where the HTTP-request ThreadLocal
 * may have already been cleared (or restored explicitly by the listener before calling us).
 */
@Slf4j
@Component
public class StudentAndProfessorUserIdAdapter extends TenantScopedRepository
        implements StudentUserIdPort, ProfessorUserIdPort {

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findUserIdByStudentId(UUID tenantId, UUID studentId) {
        applyTenantContext();
        @SuppressWarnings("unchecked")
        List<UUID> userIds = entityManager.createNativeQuery(
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
    @Transactional(readOnly = true)
    public Optional<UUID> findUserIdByProfessorId(UUID tenantId, UUID professorId) {
        applyTenantContext();
        // Step 1: resolve professor's email
        @SuppressWarnings("unchecked")
        List<String> emails = entityManager.createNativeQuery(
                        "SELECT email FROM professors WHERE id = :professorId AND tenant_id = :tenantId")
                .setParameter("professorId", professorId)
                .setParameter("tenantId", tenantId)
                .getResultList();

        if (emails.isEmpty()) {
            log.debug("No professor found for professorId={}", professorId);
            return Optional.empty();
        }
        String email = emails.get(0);

        // Step 2: find user by email (users table has no RLS — no tenant filter needed)
        @SuppressWarnings("unchecked")
        List<UUID> userIds = entityManager.createNativeQuery(
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
