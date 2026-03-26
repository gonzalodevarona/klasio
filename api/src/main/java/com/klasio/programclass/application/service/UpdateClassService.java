package com.klasio.programclass.application.service;

import com.klasio.programclass.application.dto.UpdateClassCommand;
import com.klasio.programclass.application.port.input.UpdateClassUseCase;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ClassNameAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UpdateClassService implements UpdateClassUseCase {

    private final ProgramClassRepository programClassRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateClassService(ProgramClassRepository programClassRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.programClassRepository = programClassRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ProgramClass execute(UpdateClassCommand command) {
        ProgramClass programClass = programClassRepository
                .findById(command.tenantId(), command.classId())
                .orElseThrow(() -> new ClassNotFoundException(
                        "Class with id '%s' not found".formatted(command.classId())));

        if (programClassRepository.existsByNameInProgramExcluding(
                command.programId(), command.name(), command.classId())) {
            throw new ClassNameAlreadyExistsException(
                    "A class with name '%s' already exists in this program".formatted(command.name()));
        }

        programClass.update(
                command.name(),
                command.level(),
                command.scheduleEntries(),
                command.maxStudents(),
                command.updatedBy()
        );

        List<DomainEvent> events = List.copyOf(programClass.getDomainEvents());

        programClassRepository.save(programClass);

        programClass.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return programClass;
    }
}
