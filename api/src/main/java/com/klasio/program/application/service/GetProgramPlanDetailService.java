package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramPlanDetail;
import com.klasio.program.application.port.input.GetProgramPlanDetailUseCase;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.port.ProgramPlanRepository;
import com.klasio.shared.domain.port.UserDisplayNamePort;
import com.klasio.shared.infrastructure.exception.ProgramPlanNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetProgramPlanDetailService implements GetProgramPlanDetailUseCase {

    private final ProgramPlanRepository planRepository;
    private final UserDisplayNamePort userDisplayNamePort;

    public GetProgramPlanDetailService(ProgramPlanRepository planRepository,
                                       UserDisplayNamePort userDisplayNamePort) {
        this.planRepository = planRepository;
        this.userDisplayNamePort = userDisplayNamePort;
    }

    @Override
    public ProgramPlanDetail execute(UUID tenantId, UUID planId) {
        ProgramPlan plan = planRepository.findById(tenantId, planId)
                .orElseThrow(() -> new ProgramPlanNotFoundException("Plan not found"));

        String managerName = resolveName(plan.getManagerId());
        String createdByName = resolveName(plan.getCreatedBy());
        String updatedByName = plan.getUpdatedBy() != null ? resolveName(plan.getUpdatedBy()) : null;

        return ProgramPlanDetail.fromDomain(plan, managerName, createdByName, updatedByName);
    }

    private String resolveName(UUID userId) {
        return userDisplayNamePort.findDisplayName(userId).orElse(userId.toString());
    }
}
