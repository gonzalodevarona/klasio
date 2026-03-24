package com.klasio.program.application.port.input;

import com.klasio.program.domain.model.Program;

import java.util.UUID;

public interface ReactivateProgramUseCase {

    Program execute(UUID tenantId, UUID programId, UUID reactivatedBy);
}
