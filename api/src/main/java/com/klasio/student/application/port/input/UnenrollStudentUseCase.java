package com.klasio.student.application.port.input;

import com.klasio.student.application.dto.UnenrollStudentCommand;
import com.klasio.student.domain.model.StudentEnrollment;

public interface UnenrollStudentUseCase {
    StudentEnrollment execute(UnenrollStudentCommand command);
}
