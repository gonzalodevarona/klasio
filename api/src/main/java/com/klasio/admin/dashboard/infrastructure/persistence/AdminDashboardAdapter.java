package com.klasio.admin.dashboard.infrastructure.persistence;

import com.klasio.admin.dashboard.application.dto.DashboardStudentDto;
import com.klasio.admin.dashboard.application.port.AdminDashboardRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AdminDashboardAdapter implements AdminDashboardRepository {

    private final EntityManager em;

    public AdminDashboardAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public long countStudents(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(DISTINCT u.id)
                FROM users u
                JOIN user_roles ur ON ur.user_id = u.id
                WHERE u.tenant_id = :tenantId
                  AND ur.role = 'STUDENT'
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long countNewStudentsThisMonth(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(DISTINCT u.id)
                FROM users u
                JOIN user_roles ur ON ur.user_id = u.id
                WHERE u.tenant_id = :tenantId
                  AND ur.role = 'STUDENT'
                  AND u.created_at >= DATE_TRUNC('month', NOW() AT TIME ZONE 'UTC')
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long countActiveMemberships(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                FROM memberships m
                WHERE m.tenant_id = :tenantId
                  AND m.status = 'ACTIVE'
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long sumConsumedHours(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COALESCE(SUM(m.purchased_hours - m.available_hours), 0)
                FROM memberships m
                WHERE m.tenant_id = :tenantId
                  AND m.status = 'ACTIVE'
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long countPendingProofs(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                FROM payment_proofs pp
                WHERE pp.tenant_id = :tenantId
                  AND pp.status = 'PENDING'
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long countActivePrograms(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                FROM programs p
                WHERE p.tenant_id = :tenantId
                  AND p.status = 'ACTIVE'
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DashboardStudentDto> findStudentSummaries(UUID tenantId) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT
                    u.id::text,
                    u.first_name || ' ' || u.last_name AS name,
                    p.name                              AS program_name,
                    m.status                            AS membership_status,
                    m.available_hours,
                    m.purchased_hours
                FROM users u
                LEFT JOIN LATERAL (
                    SELECT e.program_id
                    FROM student_enrollments e
                    WHERE e.student_id = u.id
                      AND e.tenant_id  = :tenantId
                      AND e.status     = 'ACTIVE'
                    ORDER BY e.enrollment_date DESC
                    LIMIT 1
                ) enr ON TRUE
                LEFT JOIN programs p ON p.id = enr.program_id AND p.tenant_id = :tenantId
                LEFT JOIN LATERAL (
                    SELECT m2.status, m2.available_hours, m2.purchased_hours, m2.updated_at
                    FROM memberships m2
                    WHERE m2.student_id = u.id
                      AND m2.tenant_id  = :tenantId
                      AND m2.status     = 'ACTIVE'
                    ORDER BY m2.updated_at DESC
                    LIMIT 1
                ) m ON TRUE
                WHERE u.tenant_id = :tenantId
                  AND EXISTS (
                      SELECT 1 FROM user_roles ur
                      WHERE ur.user_id = u.id AND ur.role = 'STUDENT'
                  )
                ORDER BY COALESCE(m.updated_at, u.created_at) DESC
                LIMIT 50
                """)
                .setParameter("tenantId", tenantId)
                .getResultList();

        return rows.stream().map(row -> new DashboardStudentDto(
                UUID.fromString((String) row[0]),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                row[4] != null ? ((Number) row[4]).intValue() : null,
                row[5] != null ? ((Number) row[5]).intValue() : null
        )).toList();
    }
}
