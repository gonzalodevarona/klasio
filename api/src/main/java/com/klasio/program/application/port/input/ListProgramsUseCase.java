package com.klasio.program.application.port.input;

import com.klasio.program.application.dto.ProgramSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ListProgramsUseCase {

    Page<ProgramSummary> execute(UUID tenantId, Pageable pageable, String status);
}
