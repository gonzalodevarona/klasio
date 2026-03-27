package com.klasio.student.application.port.input;

import com.klasio.student.domain.model.Student;

import java.util.UUID;

public interface GetStudentUseCase {
    Student execute(UUID tenantId, UUID studentId);
}
