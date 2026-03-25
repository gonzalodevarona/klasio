package com.klasio.professor.application.service;

import com.klasio.professor.application.port.input.ReactivateProfessorUseCase;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ProfessorAlreadyActiveException;
import com.klasio.shared.infrastructure.exception.ProfessorNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReactivateProfessorService implements ReactivateProfessorUseCase {

    private final ProfessorRepository professorRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReactivateProfessorService(ProfessorRepository professorRepository,
                                      ApplicationEventPublisher eventPublisher) {
        this.professorRepository = professorRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Professor execute(UUID tenantId, UUID professorId, UUID reactivatedBy) {
        Professor professor = professorRepository.findById(tenantId, professorId)
                .orElseThrow(() -> new ProfessorNotFoundException(
                        "Professor not found with id: " + professorId));

        try {
            professor.reactivate(reactivatedBy);
        } catch (IllegalStateException ex) {
            throw new ProfessorAlreadyActiveException(ex.getMessage());
        }

        List<DomainEvent> events = List.copyOf(professor.getDomainEvents());

        professorRepository.save(professor);

        professor.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return professor;
    }
}
