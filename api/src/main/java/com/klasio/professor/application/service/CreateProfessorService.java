package com.klasio.professor.application.service;

import com.klasio.professor.application.dto.CreateProfessorCommand;
import com.klasio.professor.application.port.input.CreateProfessorUseCase;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ProfessorEmailAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.ProfessorIdentityNumberAlreadyExistsException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CreateProfessorService implements CreateProfessorUseCase {

    private final ProfessorRepository professorRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateProfessorService(ProfessorRepository professorRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.professorRepository = professorRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Professor execute(CreateProfessorCommand command) {
        if (professorRepository.existsByEmailInTenant(command.tenantId(), command.email())) {
            throw new ProfessorEmailAlreadyExistsException(
                    "A professor with email '%s' already exists in this tenant".formatted(command.email()));
        }

        if (professorRepository.existsByIdentityNumberInTenant(command.tenantId(), command.identityNumber())) {
            throw new ProfessorIdentityNumberAlreadyExistsException(
                    "A professor with identity number '%s' already exists in this tenant"
                            .formatted(command.identityNumber()));
        }

        Professor professor = Professor.create(
                command.tenantId(),
                command.firstName(),
                command.lastName(),
                command.email(),
                command.phoneNumber(),
                command.identityDocumentType(),
                command.identityNumber(),
                command.createdBy()
        );

        List<DomainEvent> events = List.copyOf(professor.getDomainEvents());

        professorRepository.save(professor);

        professor.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return professor;
    }
}
