package com.klasio.program.application.port.input;

import com.klasio.program.application.dto.CreateProgramCommand;
import com.klasio.program.domain.model.Program;

public interface CreateProgramUseCase {

    Program execute(CreateProgramCommand command);
}
