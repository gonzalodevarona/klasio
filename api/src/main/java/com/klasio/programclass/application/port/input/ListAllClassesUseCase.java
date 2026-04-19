package com.klasio.programclass.application.port.input;

import com.klasio.programclass.application.dto.ClassSummary;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ListAllClassesUseCase {
    Page<ClassSummary> execute(UUID tenantId, ClassLevel level, ClassStatus status, String programName, Pageable pageable);

    /** Overload used by AllClassesController when the caller may be a PROFESSOR (applies professor scope filter). */
    Page<ClassSummary> execute(UUID tenantId, ClassLevel level, ClassStatus status, String programName,
                               Pageable pageable, UUID userId, String role);

    /** Overload used by ADMIN/MANAGER to list classes assigned to a specific professor. */
    Page<ClassSummary> execute(UUID tenantId, ClassLevel level, ClassStatus status,
                               Pageable pageable, UUID professorId);
}
