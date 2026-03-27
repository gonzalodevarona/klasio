package com.klasio.student.application.port.input;

import com.klasio.student.application.dto.PromoteStudentCommand;
import com.klasio.student.domain.model.StudentEnrollment;

public interface PromoteStudentUseCase {
    /**
     * Promotes a student to the target level.
     * Returns the active enrollment at the target level (either pre-existing or newly created).
     */
    StudentEnrollment execute(PromoteStudentCommand command);
}
