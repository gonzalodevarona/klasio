package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.EligibleStudentLookupPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Persistence adapter for {@link EligibleStudentLookupPort}.
 *
 * <p>Uses a native SQL query for maximum control over the JOIN shape and the
 * Postgres-specific ANY(CAST(... AS uuid[])) pattern that avoids an empty
 * IN() clause when {@code excludeStudentIds} is empty.
 *
 * <p>Table/column mapping (from DB introspection):
 * <ul>
 *   <li>{@code students} — id, tenant_id, first_name, last_name, identity_number</li>
 *   <li>{@code student_enrollments} — id, tenant_id, student_id, program_id, level, status</li>
 *   <li>{@code memberships} — id, tenant_id, student_id, program_id, available_hours, status</li>
 * </ul>
 */
@Component
public class EligibleStudentLookupAdapter implements EligibleStudentLookupPort {

    @PersistenceContext
    private EntityManager em;

    /**
     * Native SQL that finds students eligible for a staff walk-in registration.
     *
     * <p>Eligibility criteria (all must hold):
     * <ol>
     *   <li>Active enrollment in the given program at the given level (spe.status = 'ACTIVE')</li>
     *   <li>Active membership for the same student+program with available_hours >= minHours</li>
     *   <li>Student not in the excludeStudentIds set (handled via Postgres ANY trick)</li>
     *   <li>Optional case-insensitive name OR id-document-prefix filter</li>
     * </ol>
     *
     * <p>The exclude-set trick:
     * <pre>(:excludeStudentIdsEmpty = TRUE OR NOT (s.id = ANY(CAST(:excludeStudentIds AS uuid[]))))</pre>
     * When the set is empty we pass an empty uuid array AND set the boolean flag to TRUE,
     * so the OR short-circuits before the ANY is evaluated — avoiding Postgres's rejection
     * of an empty uuid[] IN context.
     *
     * <p>The name filter trick:
     * <pre>CAST(:nameFilter AS text) IS NULL OR ...</pre>
     * When nameFilter is null, CAST(null AS text) IS NULL = TRUE → short-circuit.
     */
    private static final String ELIGIBLE_QUERY = """
            SELECT
                s.id                   AS student_id,
                s.first_name || ' ' || s.last_name AS full_name,
                s.identity_number      AS id_document,
                spe.id                 AS enrollment_id,
                m.id                   AS membership_id,
                m.available_hours      AS available_hours
            FROM students s
            JOIN student_enrollments spe
                ON spe.student_id = s.id
                AND spe.program_id = CAST(:programId AS uuid)
                AND spe.status     = 'ACTIVE'
                AND spe.level      = :level
                AND spe.tenant_id  = CAST(:tenantId AS uuid)
            JOIN memberships m
                ON m.student_id  = s.id
                AND m.program_id = CAST(:programId AS uuid)
                AND m.status     = 'ACTIVE'
                AND (m.modality = 'UNLIMITED' OR m.available_hours >= :minHours)
                AND m.tenant_id  = CAST(:tenantId AS uuid)
            WHERE
                s.tenant_id = CAST(:tenantId AS uuid)
                AND (
                    CAST(:nameFilter AS text) IS NULL
                    OR LOWER(s.first_name || ' ' || s.last_name) LIKE LOWER('%' || :nameFilter || '%')
                    OR s.identity_number LIKE :nameFilter || '%'
                )
                AND (
                    :excludeStudentIdsEmpty = TRUE
                    OR NOT (s.id = ANY(CAST(:excludeStudentIds AS uuid[])))
                )
            ORDER BY s.first_name, s.last_name
            LIMIT :limit
            """;

    @Override
    @SuppressWarnings("unchecked")
    public List<EligibleStudentView> findEligible(UUID tenantId,
                                                   UUID programId,
                                                   String level,
                                                   int minHours,
                                                   String nameFilter,
                                                   Set<UUID> excludeStudentIds,
                                                   int limit) {
        boolean empty = excludeStudentIds == null || excludeStudentIds.isEmpty();
        // Convert set to array; Postgres requires a real array for ANY() — empty array is fine
        // because the :excludeStudentIdsEmpty boolean short-circuits before it is evaluated.
        String excludeArray = buildUuidArrayLiteral(
                empty ? Set.of() : excludeStudentIds);

        var query = em.createNativeQuery(ELIGIBLE_QUERY)
                .setParameter("tenantId", tenantId.toString())
                .setParameter("programId", programId.toString())
                .setParameter("level", level)
                .setParameter("minHours", minHours)
                .setParameter("nameFilter", nameFilter)
                .setParameter("excludeStudentIdsEmpty", empty)
                .setParameter("excludeStudentIds", excludeArray)
                .setParameter("limit", limit);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(r -> new EligibleStudentView(
                        UUID.fromString(r[0].toString()),
                        (String) r[1],
                        (String) r[2],
                        UUID.fromString(r[3].toString()),
                        UUID.fromString(r[4].toString()),
                        ((Number) r[5]).intValue()))
                .toList();
    }

    /**
     * Converts a set of UUIDs to a Postgres array literal string, e.g. {@code "{uuid1,uuid2}"}.
     * An empty set produces {@code "{}"}, which is valid Postgres syntax for an empty uuid array.
     */
    private static String buildUuidArrayLiteral(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (UUID id : ids) {
            if (!first) sb.append(',');
            sb.append(id);
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }
}
