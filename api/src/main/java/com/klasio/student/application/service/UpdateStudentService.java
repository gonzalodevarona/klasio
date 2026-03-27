package com.klasio.student.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.StudentEmailAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.StudentNotFoundException;
import com.klasio.student.application.dto.UpdateStudentCommand;
import com.klasio.student.application.port.input.UpdateStudentUseCase;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.StudentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UpdateStudentService implements UpdateStudentUseCase {

    private final StudentRepository studentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateStudentService(StudentRepository studentRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.studentRepository = studentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Student execute(UpdateStudentCommand command) {
        Student student = studentRepository.findById(command.tenantId(), command.studentId())
                .orElseThrow(() -> new StudentNotFoundException(
                        "Student with id '%s' not found".formatted(command.studentId())));

        if (studentRepository.existsByEmailInTenantExcluding(
                command.tenantId(), command.email(), command.studentId())) {
            throw new StudentEmailAlreadyExistsException(
                    "A student with email '%s' already exists in this tenant".formatted(command.email()));
        }

        student.update(
                command.firstName(), command.lastName(), command.email(),
                command.dateOfBirth(), command.eps(),
                command.identityNumber(), command.identityDocumentType(),
                command.bloodType(), command.phone(),
                command.tutorFirstName(), command.tutorLastName(),
                command.tutorRelationship(), command.tutorPhone(), command.tutorEmail(),
                command.updatedBy());

        List<DomainEvent> events = List.copyOf(student.getDomainEvents());

        studentRepository.save(student);

        student.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return student;
    }
}
