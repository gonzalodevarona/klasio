package com.klasio.programclass.application.port.input;

import com.klasio.programclass.application.dto.CreateClassCommand;
import com.klasio.programclass.domain.model.ProgramClass;

public interface CreateClassUseCase {

    ProgramClass execute(CreateClassCommand command);
}
