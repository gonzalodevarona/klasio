package com.klasio.programclass.application.port.input;

import com.klasio.programclass.application.dto.AssignProfessorCommand;
import com.klasio.programclass.domain.model.ProgramClass;

public interface AssignProfessorUseCase {

    ProgramClass execute(AssignProfessorCommand command);
}
