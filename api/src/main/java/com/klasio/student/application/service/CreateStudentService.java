package com.klasio.student.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.StudentEmailAlreadyExistsException;
import com.klasio.student.application.dto.CreateStudentCommand;
import com.klasio.student.application.port.input.CreateStudentUseCase;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.StudentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CreateStudentService implements CreateStudentUseCase {

    private final StudentRepository studentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateStudentService(StudentRepository studentRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.studentRepository = studentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Student execute(CreateStudentCommand command) {
        if (studentRepository.existsByEmailInTenant(command.tenantId(), command.email())) {
            throw new StudentEmailAlreadyExistsException(
                    "A student with email '%s' already exists in this tenant".formatted(command.email()));
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

        return student;
    }
}
