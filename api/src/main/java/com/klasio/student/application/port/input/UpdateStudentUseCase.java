package com.klasio.student.application.port.input;

import com.klasio.student.application.dto.UpdateStudentCommand;
import com.klasio.student.domain.model.Student;

public interface UpdateStudentUseCase {
    Student execute(UpdateStudentCommand command);
}
