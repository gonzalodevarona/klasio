package com.klasio.student.application.port.input;

import com.klasio.student.application.dto.StudentSummary;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ListStudentsUseCase {
    Page<StudentSummary> execute(UUID tenantId, int page, int size, String status, String search);
}
