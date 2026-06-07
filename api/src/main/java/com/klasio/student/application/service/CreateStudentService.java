package com.klasio.student.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.StudentEmailAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.StudentIdentityNumberAlreadyExistsException;
import com.klasio.student.application.dto.CreateStudentCommand;
import com.klasio.student.application.port.input.CreateStudentUseCase;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.AccountSetupCreationPort;
import com.klasio.student.domain.port.StudentRepository;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CreateStudentService implements CreateStudentUseCase {

    private final StudentRepository studentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountSetupCreationPort accountSetupCreationPort;

    public CreateStudentService(StudentRepository studentRepository,
                                ApplicationEventPublisher eventPublisher,
                                AccountSetupCreationPort accountSetupCreationPort) {
        this.studentRepository = studentRepository;
        this.eventPublisher = eventPublisher;
        this.accountSetupCreationPort = accountSetupCreationPort;
    }

    @Override
    public Student execute(CreateStudentCommand command) {
        if (studentRepository.existsByEmailInTenant(command.tenantId(), command.email())) {
            throw new StudentEmailAlreadyExistsException(
                    "A student with email '%s' already exists in this tenant".formatted(command.email()));
        }

        if (studentRepository.existsByIdentityNumberInTenant(command.tenantId(), command.identityNumber())) {
            throw new StudentIdentityNumberAlreadyExistsException(
                    "A student with identity number '%s' already exists in this tenant".formatted(command.identityNumber()));
        }

        Student student = Student.create(
                command.tenantId(),
                command.firstName(),
                command.lastName(),
                command.email(),
                command.dateOfBirth(),
                command.eps(),
                command.identityNumber(),
                command.identityDocumentType(),
                command.bloodType(),
                command.phone(),
                command.tutorFirstName(),
                command.tutorLastName(),
                command.tutorRelationship(),
                command.tutorPhone(),
                command.tutorEmail(),
                command.createdBy()
        );

        List<DomainEvent> events = List.copyOf(student.getDomainEvents());
        studentRepository.save(student);
        student.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        // Create user account in EMAIL_UNVERIFIED state and dispatch 15-min setup link.
        UUID userId = accountSetupCreationPort.createAndDispatchSetup(
                command.tenantId(),
                command.email(),
                command.firstName(),
                command.lastName(),
                command.identityDocumentType(),
                command.identityNumber(),
                command.phone()
        );
        student.linkUser(userId);
        studentRepository.save(student);

        return student;
    }
}
