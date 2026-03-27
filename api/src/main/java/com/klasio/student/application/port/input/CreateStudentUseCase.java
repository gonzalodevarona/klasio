package com.klasio.student.application.port.input;

import com.klasio.student.application.dto.CreateStudentCommand;
import com.klasio.student.domain.model.Student;

public interface CreateStudentUseCase {
    Student execute(CreateStudentCommand command);
}
