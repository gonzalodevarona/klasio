package com.klasio.program.application.port.input;

import com.klasio.program.application.dto.CreateProgramPlanCommand;
import com.klasio.program.domain.model.ProgramPlan;

public interface CreateProgramPlanUseCase {

    ProgramPlan execute(CreateProgramPlanCommand command);
}
