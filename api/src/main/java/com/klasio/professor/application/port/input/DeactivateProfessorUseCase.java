package com.klasio.professor.application.port.input;

import com.klasio.professor.domain.model.Professor;

import java.util.UUID;

public interface DeactivateProfessorUseCase {
    Professor execute(UUID tenantId, UUID professorId, UUID deactivatedBy);
}
