package com.klasio.program.application.service;

import com.klasio.program.application.dto.UpdateProgramPlanCommand;
import com.klasio.program.application.port.input.UpdateProgramPlanUseCase;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.port.ProgramPlanRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ProgramPlanNameAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.ProgramPlanNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UpdateProgramPlanService implements UpdateProgramPlanUseCase {

    private final ProgramPlanRepository planRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateProgramPlanService(ProgramPlanRepository planRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.planRepository = planRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ProgramPlan execute(UpdateProgramPlanCommand command) {
        ProgramPlan plan = planRepository.findById(command.tenantId(), command.planId())
                .orElseThrow(() -> new ProgramPlanNotFoundException("Plan not found"));

        if (planRepository.existsByNameInProgramExcluding(
                command.programId(), command.name(), command.planId())) {
            throw new ProgramPlanNameAlreadyExistsException(
                    "A plan with name '%s' already exists in this program".formatted(command.name()));
        }

        plan.update(command.name(), command.cost(), command.hours(),
                command.scheduleEntries(), command.managerId(), command.updatedBy());

        List<DomainEvent> events = List.copyOf(plan.getDomainEvents());

        planRepository.save(plan);

        plan.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return plan;
    }
}
