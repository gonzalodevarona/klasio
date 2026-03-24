package com.klasio.program.application.port.input;

import com.klasio.program.application.dto.ProgramPlanSummary;

import java.util.List;
import java.util.UUID;

public interface ListAllPlansUseCase {

    List<ProgramPlanSummary> execute(UUID tenantId, String status);
}
