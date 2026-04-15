package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.port.StudentNamePort;
import com.klasio.membership.domain.port.StudentProfilePort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class StudentNameAdapter implements StudentNamePort, StudentProfilePort {

    private final EntityManager em;

    public StudentNameAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<String> findFullName(UUID studentId, UUID tenantId) {
        return findProfile(studentId, tenantId).map(StudentProfile::fullName);
    }

    @Override
    public Optional<StudentProfile> findProfile(UUID studentId, UUID tenantId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                        """
                        SELECT s.firstName, s.lastName, s.identityDocumentType, s.identityNumber
                        FROM StudentJpaEntity s
                        WHERE s.id = :studentId
                          AND s.tenantId = :tenantId
                        """)
                .setParameter("studentId", studentId)
                .setParameter("tenantId", tenantId)
                .getResultList();

        if (rows.isEmpty()) return Optional.empty();
        Object[] row = rows.get(0);
        return Optional.of(new StudentProfile(
                row[0] + " " + row[1],
                (String) row[2],
                (String) row[3]
        ));
    }
}
