package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramSummary;
import com.klasio.program.application.port.input.ListProgramsUseCase;
import com.klasio.program.domain.model.ProgramStatus;
import com.klasio.program.domain.port.ProgramRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListProgramsService implements ListProgramsUseCase {

    private final ProgramRepository programRepository;

    public ListProgramsService(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    @Override
    public Page<ProgramSummary> execute(UUID tenantId, Pageable pageable, String status) {
        ProgramStatus parsedStatus = status != null ? ProgramStatus.valueOf(status) : null;
        return programRepository.findAllByTenant(tenantId, pageable, parsedStatus)
                .map(ProgramSummary::fromDomain);
    }
}
