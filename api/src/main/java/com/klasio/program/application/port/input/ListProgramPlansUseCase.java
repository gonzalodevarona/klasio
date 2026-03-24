package com.klasio.program.application.port.input;

import com.klasio.program.application.dto.ProgramPlanSummary;

import java.util.List;
import java.util.UUID;

public interface ListProgramPlansUseCase {

    List<ProgramPlanSummary> execute(UUID tenantId, UUID programId);
}
