package com.klasio.professor.application.port.input;

import com.klasio.professor.domain.model.Professor;

import java.util.UUID;

public interface ReactivateProfessorUseCase {
    Professor execute(UUID tenantId, UUID professorId, UUID reactivatedBy);
}
