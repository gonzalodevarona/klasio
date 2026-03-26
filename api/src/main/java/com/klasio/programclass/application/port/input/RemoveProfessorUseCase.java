package com.klasio.programclass.application.port.input;

import com.klasio.programclass.domain.model.ProgramClass;

import java.util.UUID;

public interface RemoveProfessorUseCase {
    ProgramClass execute(UUID tenantId, UUID classId, UUID removedBy);
}
