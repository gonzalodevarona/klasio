package com.klasio.program.application.port.input;

import com.klasio.program.application.dto.UpdateProgramPlanCommand;
import com.klasio.program.domain.model.ProgramPlan;

public interface UpdateProgramPlanUseCase {

    ProgramPlan execute(UpdateProgramPlanCommand command);
}
