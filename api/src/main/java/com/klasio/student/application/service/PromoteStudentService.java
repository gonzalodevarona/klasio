package com.klasio.student.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.EnrollmentAlreadyInactiveException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.student.application.dto.PromoteStudentCommand;
import com.klasio.student.application.port.input.PromoteStudentUseCase;
import com.klasio.student.domain.model.Level;
import com.klasio.student.domain.model.LevelHistoryEntry;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.port.LevelHistoryRepository;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PromoteStudentService implements PromoteStudentUseCase {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final LevelHistoryRepository levelHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PromoteStudentService(StudentEnrollmentRepository enrollmentRepository,
                                 LevelHistoryRepository levelHistoryRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.enrollmentRepository = enrollmentRepository;
        this.levelHistoryRepository = levelHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public StudentEnrollment execute(PromoteStudentCommand command) {
        // 1. Load source enrollment
        StudentEnrollment source = enrollmentRepository
                .findById(command.tenantId(), command.enrollmentId())
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "Enrollment with id '%s' not found".formatted(command.enrollmentId())));

        if ("INACTIVE".equals(source.getStatus())) {
            throw new EnrollmentAlreadyInactiveException(
                    "Enrollment '%s' is already inactive".formatted(command.enrollmentId()));
        }

        if (source.getLevel() == command.targetLevel()) {
            throw new IllegalArgumentException(
                    "Student is already enrolled at level '%s'".formatted(command.targetLevel()));
        }

        Level previousLevel = source.getLevel();

        // 2. Check if target level enrollment already exists
        Optional<StudentEnrollment> existingTarget = enrollmentRepository
                .findActiveByStudentIdAndProgramIdAndLevel(
                        command.tenantId(),
                        source.getStudentId(),
                        source.getProgramId(),
                        command.targetLevel().name());

        List<DomainEvent> eventsToPublish = new ArrayList<>();

        // 3. Deactivate source enrollment
        source.deactivateForPromotion(command.changedBy());
        eventsToPublish.addAll(source.getDomainEvents());
        enrollmentRepository.save(source);
        source.clearDomainEvents();

        StudentEnrollment targetEnrollment;

        if (existingTarget.isPresent()) {
            // 4a. Target level already enrolled — just record history on it
            targetEnrollment = existingTarget.get();
            LevelHistoryEntry historyEntry = LevelHistoryEntry.createPromotion(
                    command.tenantId(),
                    targetEnrollment.getId().value(),
                    previousLevel,
                    command.targetLevel(),
                    command.changedBy(),
                    command.changedByRole()
            );
            levelHistoryRepository.save(historyEntry);
        } else {
            // 4b. Create new enrollment at target level
            targetEnrollment = StudentEnrollment.create(
                    command.tenantId(),
                    source.getStudentId(),
                    source.getProgramId(),
                    command.targetLevel(),
                    command.changedBy()
            );
            eventsToPublish.addAll(targetEnrollment.getDomainEvents());
            enrollmentRepository.save(targetEnrollment);
            targetEnrollment.clearDomainEvents();

            LevelHistoryEntry historyEntry = LevelHistoryEntry.createPromotion(
                    command.tenantId(),
                    targetEnrollment.getId().value(),
                    previousLevel,
                    command.targetLevel(),
                    command.changedBy(),
                    command.changedByRole()
            );
            levelHistoryRepository.save(historyEntry);
        }

        eventsToPublish.forEach(eventPublisher::publishEvent);

        return targetEnrollment;
    }
}
