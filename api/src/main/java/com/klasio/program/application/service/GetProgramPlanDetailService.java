package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramPlanDetail;
import com.klasio.program.application.port.input.GetProgramPlanDetailUseCase;
import com.klasio.program.domain.port.ProgramPlanRepository;
import com.klasio.shared.infrastructure.exception.ProgramPlanNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetProgramPlanDetailService implements GetProgramPlanDetailUseCase {

    private final ProgramPlanRepository planRepository;

    public GetProgramPlanDetailService(ProgramPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public ProgramPlanDetail execute(UUID tenantId, UUID planId) {
        return planRepository.findById(tenantId, planId)
                .map(ProgramPlanDetail::fromDomain)
                .orElseThrow(() -> new ProgramPlanNotFoundException("Plan not found"));
    }
}
