package com.klasio.student.application.port.input;

import com.klasio.student.application.dto.EnrollmentSummary;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ListEnrollmentsUseCase {

    Page<EnrollmentSummary> byProgram(UUID tenantId, UUID programId, int page, int size, String level, String status);

    Page<EnrollmentSummary> byStudent(UUID tenantId, UUID studentId, int page, int size, String status);
}
