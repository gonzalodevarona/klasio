package com.klasio.professor.application.port.input;

import com.klasio.professor.application.dto.ProfessorDetail;

import java.util.UUID;

public interface GetProfessorDetailUseCase {
    ProfessorDetail execute(UUID tenantId, UUID professorId);
}
