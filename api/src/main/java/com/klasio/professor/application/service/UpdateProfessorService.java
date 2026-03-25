package com.klasio.professor.application.service;

import com.klasio.professor.application.dto.UpdateProfessorCommand;
import com.klasio.professor.application.port.input.UpdateProfessorUseCase;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ProfessorEmailAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.ProfessorNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UpdateProfessorService implements UpdateProfessorUseCase {

    private final ProfessorRepository professorRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateProfessorService(ProfessorRepository professorRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.professorRepository = professorRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Professor execute(UpdateProfessorCommand command) {
        Professor professor = professorRepository.findById(command.tenantId(), command.professorId())
                .orElseThrow(() -> new ProfessorNotFoundException(
                        "Professor not found with id: " + command.professorId()));

        if (professorRepository.existsByEmailInTenantExcluding(
                command.tenantId(), command.email(), command.professorId())) {
            throw new ProfessorEmailAlreadyExistsException(
                    "A professor with email '%s' already exists in this tenant".formatted(command.email()));
        }

        professor.update(command.firstName(), command.lastName(), command.email(), command.phoneNumber(), command.updatedBy());

        List<DomainEvent> events = List.copyOf(professor.getDomainEvents());

        professorRepository.save(professor);

        professor.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return professor;
    }
}
