package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramPlanSummary;
import com.klasio.program.application.port.input.ListProgramPlansUseCase;
import com.klasio.program.domain.port.ProgramPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListProgramPlansService implements ListProgramPlansUseCase {

    private final ProgramPlanRepository planRepository;

    public ListProgramPlansService(ProgramPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public List<ProgramPlanSummary> execute(UUID tenantId, UUID programId) {
        return planRepository.findAllByProgram(tenantId, programId).stream()
                .map(ProgramPlanSummary::fromDomain)
                .toList();
    }
}
