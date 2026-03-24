package com.klasio.program.application.service;

import com.klasio.program.application.dto.UpdateProgramCommand;
import com.klasio.program.application.port.input.UpdateProgramUseCase;
import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ProgramNameAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.ProgramNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UpdateProgramService implements UpdateProgramUseCase {

    private final ProgramRepository programRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateProgramService(ProgramRepository programRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.programRepository = programRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Program execute(UpdateProgramCommand command) {
        Program program = programRepository.findById(command.tenantId(), command.programId())
                .orElseThrow(() -> new ProgramNotFoundException("Program not found"));

        if (programRepository.existsByNameInTenantExcluding(
                command.tenantId(), command.name(), command.programId())) {
            throw new ProgramNameAlreadyExistsException(
                    "A program with name '%s' already exists in this tenant".formatted(command.name()));
        }

        program.update(command.name(), command.updatedBy());

        List<DomainEvent> events = List.copyOf(program.getDomainEvents());

        programRepository.save(program);

        program.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return program;
    }
}
