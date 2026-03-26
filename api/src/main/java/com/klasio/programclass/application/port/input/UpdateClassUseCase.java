package com.klasio.programclass.application.port.input;

import com.klasio.programclass.application.dto.UpdateClassCommand;
import com.klasio.programclass.domain.model.ProgramClass;

public interface UpdateClassUseCase {
    ProgramClass execute(UpdateClassCommand command);
}
