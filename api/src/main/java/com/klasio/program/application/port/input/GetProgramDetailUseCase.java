package com.klasio.program.application.port.input;

import com.klasio.program.application.dto.ProgramDetail;

import java.util.UUID;

public interface GetProgramDetailUseCase {

    ProgramDetail execute(UUID tenantId, UUID programId);
}
