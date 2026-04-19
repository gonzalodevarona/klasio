package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.ClassSessionRosterView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ListClassSessionRosterUseCase {

    /**
     * Returns sessions in [from, to] for the given class with their registered students.
     *
     * @param tenantId        tenant from JWT/interceptor
     * @param classId         target class
     * @param from            start of date range (inclusive)
     * @param to              end of date range (inclusive, max 30 days from 'from')
     * @param role            caller's role string (ADMIN, SUPERADMIN, MANAGER, PROFESSOR)
     * @param userId          caller's user UUID from JWT
     * @param programIdFromJwt programId claim from JWT (non-null for MANAGER, null otherwise)
     */
    List<ClassSessionRosterView> execute(UUID tenantId, UUID classId,
                                         LocalDate from, LocalDate to,
                                         String role, UUID userId,
                                         UUID programIdFromJwt);
}
