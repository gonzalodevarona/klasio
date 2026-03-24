package com.klasio.program.application.port.input;

import com.klasio.program.application.dto.ProgramPlanDetail;

import java.util.UUID;

public interface GetProgramPlanDetailUseCase {

    ProgramPlanDetail execute(UUID tenantId, UUID planId);
}
