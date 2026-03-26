package com.klasio.programclass.application.service;

import com.klasio.programclass.application.dto.CreateClassCommand;
import com.klasio.programclass.application.port.input.CreateClassUseCase;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ClassNameAlreadyExistsException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CreateClassService implements CreateClassUseCase {

    private final ProgramClassRepository programClassRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateClassService(ProgramClassRepository programClassRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.programClassRepository = programClassRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ProgramClass execute(CreateClassCommand command) {
        if (programClassRepository.existsByNameInProgram(command.programId(), command.name())) {
            throw new ClassNameAlreadyExistsException(
                    "A class with name '%s' already exists in this program".formatted(command.name()));
        }

        ProgramClass programClass = ProgramClass.create(
                command.tenantId(),
                command.programId(),
                command.name(),
                command.level(),
                command.type(),
                command.scheduleEntries(),
                command.professorId(),
                command.maxStudents(),
                command.createdBy()
        );

        List<DomainEvent> events = List.copyOf(programClass.getDomainEvents());

        programClassRepository.save(programClass);

        programClass.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return programClass;
    }
}
