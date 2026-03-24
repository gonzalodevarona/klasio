package com.klasio.program.application.port.input;

import com.klasio.program.domain.model.Program;

import java.util.UUID;

public interface DeactivateProgramUseCase {

    Program execute(UUID tenantId, UUID programId, UUID deactivatedBy);
}
