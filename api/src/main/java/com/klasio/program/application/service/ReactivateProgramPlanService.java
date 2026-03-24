package com.klasio.program.application.service;

import com.klasio.program.application.port.input.ReactivateProgramPlanUseCase;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.port.ProgramPlanRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ProgramPlanNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReactivateProgramPlanService implements ReactivateProgramPlanUseCase {

    private final ProgramPlanRepository planRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReactivateProgramPlanService(ProgramPlanRepository planRepository,
                                        ApplicationEventPublisher eventPublisher) {
        this.planRepository = planRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ProgramPlan execute(UUID tenantId, UUID planId, UUID reactivatedBy) {
        ProgramPlan plan = planRepository.findById(tenantId, planId)
                .orElseThrow(() -> new ProgramPlanNotFoundException("Plan not found"));

        plan.reactivate(reactivatedBy);

        List<DomainEvent> events = List.copyOf(plan.getDomainEvents());

        planRepository.save(plan);

        plan.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return plan;
    }
}
