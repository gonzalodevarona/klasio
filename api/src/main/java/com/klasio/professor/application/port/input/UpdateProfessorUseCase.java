package com.klasio.professor.application.port.input;

import com.klasio.professor.application.dto.UpdateProfessorCommand;
import com.klasio.professor.domain.model.Professor;

public interface UpdateProfessorUseCase {
    Professor execute(UpdateProfessorCommand command);
}
