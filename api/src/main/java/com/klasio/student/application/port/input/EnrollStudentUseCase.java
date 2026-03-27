package com.klasio.student.application.port.input;

import com.klasio.student.application.dto.EnrollStudentCommand;
import com.klasio.student.domain.model.StudentEnrollment;

public interface EnrollStudentUseCase {
    StudentEnrollment execute(EnrollStudentCommand command);
}
