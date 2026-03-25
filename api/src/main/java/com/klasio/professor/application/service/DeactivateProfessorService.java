package com.klasio.professor.application.service;

import com.klasio.professor.application.port.input.DeactivateProfessorUseCase;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ProfessorAlreadyInactiveException;
import com.klasio.shared.infrastructure.exception.ProfessorNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeactivateProfessorService implements DeactivateProfessorUseCase {

    private final ProfessorRepository professorRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DeactivateProfessorService(ProfessorRepository professorRepository,
                                      ApplicationEventPublisher eventPublisher) {
        this.professorRepository = professorRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Professor execute(UUID tenantId, UUID professorId, UUID deactivatedBy) {
        Professor professor = professorRepository.findById(tenantId, professorId)
                .orElseThrow(() -> new ProfessorNotFoundException(
                        "Professor not found with id: " + professorId));

        try {
            professor.deactivate(deactivatedBy);
        } catch (IllegalStateException ex) {
            throw new ProfessorAlreadyInactiveException(ex.getMessage());
        }

        List<DomainEvent> events = List.copyOf(professor.getDomainEvents());

        professorRepository.save(professor);

        professor.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return professor;
    }
}
