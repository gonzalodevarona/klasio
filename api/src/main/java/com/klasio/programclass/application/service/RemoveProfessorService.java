package com.klasio.programclass.application.service;

import com.klasio.programclass.application.port.input.RemoveProfessorUseCase;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RemoveProfessorService implements RemoveProfessorUseCase {

    private final ProgramClassRepository programClassRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RemoveProfessorService(ProgramClassRepository programClassRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.programClassRepository = programClassRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ProgramClass execute(UUID tenantId, UUID classId, UUID removedBy) {
        ProgramClass programClass = programClassRepository
                .findById(tenantId, classId)
                .orElseThrow(() -> new ClassNotFoundException(
                        "Class with id '%s' not found".formatted(classId)));

        programClass.removeProfessor(removedBy);

        List<DomainEvent> events = List.copyOf(programClass.getDomainEvents());

        programClassRepository.save(programClass);

        programClass.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return programClass;
    }
}
