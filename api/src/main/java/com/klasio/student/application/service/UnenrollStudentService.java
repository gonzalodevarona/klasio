package com.klasio.student.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.EnrollmentAlreadyInactiveException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.student.application.dto.UnenrollStudentCommand;
import com.klasio.student.application.port.input.UnenrollStudentUseCase;
import com.klasio.student.domain.model.LevelHistoryEntry;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.port.LevelHistoryRepository;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UnenrollStudentService implements UnenrollStudentUseCase {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final LevelHistoryRepository levelHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UnenrollStudentService(StudentEnrollmentRepository enrollmentRepository,
                                  LevelHistoryRepository levelHistoryRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.enrollmentRepository = enrollmentRepository;
        this.levelHistoryRepository = levelHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public StudentEnrollment execute(UnenrollStudentCommand command) {
        StudentEnrollment enrollment = enrollmentRepository
                .findById(command.tenantId(), command.enrollmentId())
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "Enrollment with id '%s' not found".formatted(command.enrollmentId())));

        if ("INACTIVE".equals(enrollment.getStatus())) {
            throw new EnrollmentAlreadyInactiveException(
                    "Enrollment '%s' is already inactive".formatted(command.enrollmentId()));
        }

        LevelHistoryEntry historyEntry = LevelHistoryEntry.createUnenrollment(
                command.tenantId(),
                enrollment.getId().value(),
                enrollment.getLevel(),
                command.changedBy(),
                command.changedByRole()
        );

        enrollment.unenroll(command.changedBy());

        List<DomainEvent> events = List.copyOf(enrollment.getDomainEvents());

        enrollmentRepository.save(enrollment);
        levelHistoryRepository.save(historyEntry);

        enrollment.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return enrollment;
    }
}
