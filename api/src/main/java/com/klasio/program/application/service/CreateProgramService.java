package com.klasio.program.application.service;

import com.klasio.program.application.dto.CreateProgramCommand;
import com.klasio.program.application.port.input.CreateProgramUseCase;
import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ProgramNameAlreadyExistsException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CreateProgramService implements CreateProgramUseCase {

    private final ProgramRepository programRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateProgramService(ProgramRepository programRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.programRepository = programRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Program execute(CreateProgramCommand command) {
        if (programRepository.existsByNameInTenant(command.tenantId(), command.name())) {
            throw new ProgramNameAlreadyExistsException(
                    "A program with name '%s' already exists in this tenant".formatted(command.name()));
        }

        Program program = Program.create(
                command.tenantId(),
                command.name(),
                command.dropInPrice(),
                command.createdBy()
        );

        List<DomainEvent> events = List.copyOf(program.getDomainEvents());

        programRepository.save(program);

        program.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return program;
    }
}
