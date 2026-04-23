package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramPlanSummary;
import com.klasio.program.application.port.input.ListProgramPlansUseCase;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.port.ProgramPlanRepository;
import com.klasio.shared.domain.port.UserDisplayNamePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListProgramPlansService implements ListProgramPlansUseCase {

    private final ProgramPlanRepository planRepository;
    private final UserDisplayNamePort userDisplayNamePort;

    public ListProgramPlansService(ProgramPlanRepository planRepository,
                                   UserDisplayNamePort userDisplayNamePort) {
        this.planRepository = planRepository;
        this.userDisplayNamePort = userDisplayNamePort;
    }

    @Override
    public List<ProgramPlanSummary> execute(UUID tenantId, UUID programId) {
        List<ProgramPlan> plans = planRepository.findAllByProgram(tenantId, programId);
        Map<UUID, String> managerNames = resolveManagerNames(plans);
        return plans.stream()
                .map(plan -> ProgramPlanSummary.fromDomain(plan, managerNames.get(plan.getManagerId())))
                .toList();
    }

    private Map<UUID, String> resolveManagerNames(List<ProgramPlan> plans) {
        return plans.stream()
                .map(ProgramPlan::getManagerId)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> userDisplayNamePort.findDisplayName(id).orElse(id.toString())
                ));
    }
}
