package com.klasio.student.application.port.input;

import com.klasio.student.application.dto.StudentDetail;

import java.util.UUID;

public interface GetStudentDetailUseCase {
    StudentDetail execute(UUID tenantId, UUID studentId);
}
