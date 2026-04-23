package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramDetail;
import com.klasio.program.application.port.input.GetProgramDetailUseCase;
import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.domain.port.UserDisplayNamePort;
import com.klasio.shared.infrastructure.exception.ProgramNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetProgramDetailService implements GetProgramDetailUseCase {

    private final ProgramRepository programRepository;
    private final UserDisplayNamePort userDisplayNamePort;

    public GetProgramDetailService(ProgramRepository programRepository,
                                   UserDisplayNamePort userDisplayNamePort) {
        this.programRepository = programRepository;
        this.userDisplayNamePort = userDisplayNamePort;
    }

    @Override
    public ProgramDetail execute(UUID tenantId, UUID programId) {
        Program program = programRepository.findById(tenantId, programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found"));

        String createdByName = resolveName(program.getCreatedBy());
        String updatedByName = program.getUpdatedBy() != null ? resolveName(program.getUpdatedBy()) : null;

        return ProgramDetail.fromDomain(program, createdByName, updatedByName);
    }

    private String resolveName(UUID userId) {
        return userDisplayNamePort.findDisplayName(userId).orElse(userId.toString());
    }
}
