package com.klasio.shared.infrastructure.persistence;

import com.klasio.shared.domain.port.UserDisplayNamePort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserDisplayNameAdapter implements UserDisplayNamePort {

    private final EntityManager em;

    public UserDisplayNameAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<String> findDisplayName(UUID userId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                        """
                        SELECT u.firstName, u.lastName
                        FROM UserJpaEntity u
                        WHERE u.id = :userId
                        """)
                .setParameter("userId", userId)
                .getResultList();

        if (rows.isEmpty()) return Optional.empty();
        Object[] row = rows.get(0);
        String firstName = (String) row[0];
        String lastName = (String) row[1];
        if (firstName == null && lastName == null) return Optional.empty();
        if (firstName == null) return Optional.of(lastName);
        if (lastName == null) return Optional.of(firstName);
        return Optional.of(firstName + " " + lastName);
    }
}
