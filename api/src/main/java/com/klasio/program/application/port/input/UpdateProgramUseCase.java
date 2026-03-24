package com.klasio.program.application.port.input;

import com.klasio.program.application.dto.UpdateProgramCommand;
import com.klasio.program.domain.model.Program;

public interface UpdateProgramUseCase {

    Program execute(UpdateProgramCommand command);
}
