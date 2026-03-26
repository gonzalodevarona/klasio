package com.klasio.programclass.application.service;

import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.model.ProfessorStatus;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.programclass.application.dto.AssignProfessorCommand;
import com.klasio.programclass.application.port.input.AssignProfessorUseCase;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.ProfessorNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AssignProfessorService implements AssignProfessorUseCase {

    private final ProgramClassRepository programClassRepository;
    private final ProfessorRepository professorRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AssignProfessorService(ProgramClassRepository programClassRepository,
                                  ProfessorRepository professorRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.programClassRepository = programClassRepository;
        this.professorRepository = professorRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ProgramClass execute(AssignProfessorCommand command) {
        ProgramClass programClass = programClassRepository
                .findById(command.tenantId(), command.classId())
                .orElseThrow(() -> new ClassNotFoundException(
                        "Class with id '%s' not found".formatted(command.classId())));

        Professor professor = professorRepository
                .findById(command.tenantId(), command.professorId())
                .orElseThrow(() -> new ProfessorNotFoundException(
                        "Professor with id '%s' not found".formatted(command.professorId())));

        if (professor.getStatus() == ProfessorStatus.DEACTIVATED) {
            throw new IllegalArgumentException(
                    "Cannot assign deactivated professor '%s' to a class".formatted(command.professorId()));
        }

        programClass.assignProfessor(command.professorId(), command.assignedBy());

        List<DomainEvent> events = List.copyOf(programClass.getDomainEvents());

        programClassRepository.save(programClass);

        programClass.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return programClass;
    }
}
