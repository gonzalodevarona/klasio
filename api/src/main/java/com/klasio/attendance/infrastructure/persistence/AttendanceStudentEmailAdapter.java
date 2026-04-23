package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.StudentEmailPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AttendanceStudentEmailAdapter implements StudentEmailPort {

    private final EntityManager em;

    public AttendanceStudentEmailAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<String> findEmail(UUID studentId, UUID tenantId) {
        List<String> rows = em.createQuery(
                        "SELECT s.email FROM StudentJpaEntity s WHERE s.id = :id AND s.tenantId = :tenantId",
                        String.class)
                .setParameter("id", studentId)
                .setParameter("tenantId", tenantId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
