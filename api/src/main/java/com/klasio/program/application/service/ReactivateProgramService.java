package com.klasio.program.application.service;

import com.klasio.program.application.port.input.ReactivateProgramUseCase;
import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ProgramAlreadyActiveException;
import com.klasio.shared.infrastructure.exception.ProgramNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReactivateProgramService implements ReactivateProgramUseCase {

    private final ProgramRepository programRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReactivateProgramService(ProgramRepository programRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.programRepository = programRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Program execute(UUID tenantId, UUID programId, UUID reactivatedBy) {
        Program program = programRepository.findById(tenantId, programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found"));

        try {
            program.reactivate(reactivatedBy);
        } catch (IllegalStateException e) {
            throw new ProgramAlreadyActiveException("Program is already active");
        }

        List<DomainEvent> events = List.copyOf(program.getDomainEvents());

        programRepository.save(program);

        program.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return program;
    }
}
