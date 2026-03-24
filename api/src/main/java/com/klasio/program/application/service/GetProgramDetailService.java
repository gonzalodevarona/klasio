package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramDetail;
import com.klasio.program.application.port.input.GetProgramDetailUseCase;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.infrastructure.exception.ProgramNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetProgramDetailService implements GetProgramDetailUseCase {

    private final ProgramRepository programRepository;

    public GetProgramDetailService(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    @Override
    public ProgramDetail execute(UUID tenantId, UUID programId) {
        return programRepository.findById(tenantId, programId)
                .map(ProgramDetail::fromDomain)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found"));
    }
}
