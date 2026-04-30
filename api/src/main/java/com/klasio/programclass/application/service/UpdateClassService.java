package com.klasio.programclass.application.service;

import com.klasio.attendance.application.dto.CancelMismatchingFutureRegistrationsCommand;
import com.klasio.attendance.application.port.input.CancelMismatchingFutureRegistrationsUseCase;
import com.klasio.programclass.application.dto.UpdateClassCommand;
import com.klasio.programclass.application.port.input.UpdateClassUseCase;
import com.klasio.programclass.domain.model.ClassLevel;
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
    private final CancelMismatchingFutureRegistrationsUseCase cancelMismatchingFutureRegistrationsUseCase;

    public UpdateClassService(ProgramClassRepository programClassRepository,
                              ApplicationEventPublisher eventPublisher,
                              CancelMismatchingFutureRegistrationsUseCase cancelMismatchingFutureRegistrationsUseCase) {
        this.programClassRepository = programClassRepository;
        this.eventPublisher = eventPublisher;
        this.cancelMismatchingFutureRegistrationsUseCase = cancelMismatchingFutureRegistrationsUseCase;
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

        // Capture previous level BEFORE the update so we can detect OPEN→specific transitions
        ClassLevel previousLevel = programClass.getLevel();

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

        // When a class is narrowed from OPEN to a specific level, cancel future registrations
        // of students whose enrollment level no longer matches. Runs in the same transaction —
        // if the cascade fails the entire update rolls back.
        if (previousLevel == ClassLevel.OPEN && command.level() != ClassLevel.OPEN) {
            cancelMismatchingFutureRegistrationsUseCase.execute(
                    new CancelMismatchingFutureRegistrationsCommand(
                            command.tenantId(),
                            command.classId(),
                            previousLevel.name(),
                            command.level().name(),
                            command.updatedBy()));
        }

        return programClass;
    }
}
