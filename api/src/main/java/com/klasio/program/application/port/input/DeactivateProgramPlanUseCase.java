package com.klasio.program.application.port.input;

import com.klasio.program.domain.model.ProgramPlan;

import java.util.UUID;

public interface DeactivateProgramPlanUseCase {

    ProgramPlan execute(UUID tenantId, UUID planId, UUID deactivatedBy);
}
