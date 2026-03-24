package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramPlanSummary;
import com.klasio.program.application.port.input.ListAllPlansUseCase;
import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.model.ProgramPlanStatus;
import com.klasio.program.domain.port.ProgramPlanRepository;
import com.klasio.program.domain.port.ProgramRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListAllPlansService implements ListAllPlansUseCase {

    private final ProgramPlanRepository planRepository;
    private final ProgramRepository programRepository;

    public ListAllPlansService(ProgramPlanRepository planRepository,
                               ProgramRepository programRepository) {
        this.planRepository = planRepository;
        this.programRepository = programRepository;
    }

    @Override
    public List<ProgramPlanSummary> execute(UUID tenantId, String status) {
        ProgramPlanStatus planStatus = status != null ? ProgramPlanStatus.valueOf(status) : null;

        List<ProgramPlan> plans = planRepository.findAllByTenant(tenantId, planStatus);

        Map<UUID, String> programNames = plans.stream()
                .map(ProgramPlan::getProgramId)
                .distinct()
                .map(programId -> programRepository.findById(tenantId, programId).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(
                        p -> p.getId().value(),
                        Program::getName
                ));

        return plans.stream()
                .map(plan -> ProgramPlanSummary.fromDomain(plan, programNames.get(plan.getProgramId())))
                .toList();
    }
}
