package com.klasio.professor.application.port.input;

import com.klasio.professor.application.dto.CreateProfessorCommand;
import com.klasio.professor.domain.model.Professor;

public interface CreateProfessorUseCase {
    Professor execute(CreateProfessorCommand command);
}
