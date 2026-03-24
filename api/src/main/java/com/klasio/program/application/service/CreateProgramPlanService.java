package com.klasio.program.application.service;

import com.klasio.program.application.dto.CreateProgramPlanCommand;
import com.klasio.program.application.port.input.CreateProgramPlanUseCase;
import com.klasio.program.domain.model.ProgramModality;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.port.ProgramPlanRepository;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ProgramNotFoundException;
import com.klasio.shared.infrastructure.exception.ProgramPlanNameAlreadyExistsException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CreateProgramPlanService implements CreateProgramPlanUseCase {

    private final ProgramRepository programRepository;
    private final ProgramPlanRepository planRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateProgramPlanService(ProgramRepository programRepository,
                                    ProgramPlanRepository planRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.programRepository = programRepository;
        this.planRepository = planRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ProgramPlan execute(CreateProgramPlanCommand command) {
        if (programRepository.findById(command.tenantId(), command.programId()).isEmpty()) {
            throw new ProgramNotFoundException("Program not found");
        }

        if (planRepository.existsByNameInProgram(command.programId(), command.name())) {
            throw new ProgramPlanNameAlreadyExistsException(
                    "A plan with name '%s' already exists in this program".formatted(command.name()));
        }

        ProgramModality modality = ProgramModality.valueOf(command.modality());

        ProgramPlan plan = ProgramPlan.create(
                command.programId(),
                command.tenantId(),
                command.name(),
                modality,
                command.cost(),
                command.hours(),
                command.scheduleEntries(),
                command.managerId(),
                command.createdBy()
        );

        List<DomainEvent> events = List.copyOf(plan.getDomainEvents());

        planRepository.save(plan);

        plan.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return plan;
    }
}
