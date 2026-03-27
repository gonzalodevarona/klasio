package com.klasio.student.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.EnrollmentAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.StudentNotFoundException;
import com.klasio.student.application.dto.EnrollStudentCommand;
import com.klasio.student.application.port.input.EnrollStudentUseCase;
import com.klasio.student.domain.model.LevelHistoryEntry;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.port.LevelHistoryRepository;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import com.klasio.student.domain.port.StudentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EnrollStudentService implements EnrollStudentUseCase {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final LevelHistoryRepository levelHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EnrollStudentService(StudentRepository studentRepository,
                                StudentEnrollmentRepository enrollmentRepository,
                                LevelHistoryRepository levelHistoryRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.levelHistoryRepository = levelHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public StudentEnrollment execute(EnrollStudentCommand command) {
        // 1. Check student exists and is active
        Student student = studentRepository.findById(command.tenantId(), command.studentId())
                .orElseThrow(() -> new StudentNotFoundException(
                        "Student with id '%s' not found".formatted(command.studentId())));

        if (!"ACTIVE".equals(student.getStatus())) {
            throw new StudentNotFoundException(
                    "Student with id '%s' is not active".formatted(command.studentId()));
        }

        // 2. Check no active enrollment for student+program+level
        if (enrollmentRepository.existsByStudentIdAndProgramIdAndLevelActive(
                command.studentId(), command.programId(), command.level().name())) {
            throw new EnrollmentAlreadyExistsException(
                    "Student '%s' already has an active %s enrollment in program '%s'"
                            .formatted(command.studentId(), command.level().name(), command.programId()));
        }

        // 3. Create enrollment
        StudentEnrollment enrollment = StudentEnrollment.create(
                command.tenantId(),
                command.studentId(),
                command.programId(),
                command.level(),
                command.createdBy()
        );

        // 4. Create initial level history entry
        LevelHistoryEntry historyEntry = LevelHistoryEntry.createInitial(
                command.tenantId(),
                enrollment.getId().value(),
                command.level(),
                command.createdBy(),
                command.changedByRole()
        );

        // 5. Save enrollment and history
        List<DomainEvent> events = List.copyOf(enrollment.getDomainEvents());

        enrollmentRepository.save(enrollment);
        levelHistoryRepository.save(historyEntry);

        // 6. Publish events
        enrollment.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return enrollment;
    }
}
