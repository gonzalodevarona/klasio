package com.klasio.professor.application.port.input;

import com.klasio.professor.application.dto.ProfessorSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ListProfessorsUseCase {
    Page<ProfessorSummary> execute(UUID tenantId, Pageable pageable, String status);
}
