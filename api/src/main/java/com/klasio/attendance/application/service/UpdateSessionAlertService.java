package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.dto.UpdateSessionAlertCommand;
import com.klasio.attendance.application.port.input.UpdateSessionAlertUseCase;
import com.klasio.attendance.domain.exception.AlertAuthorViolationException;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.InvalidAlertReasonException;
import com.klasio.shared.infrastructure.exception.NotAlertAuthorException;
import com.klasio.shared.infrastructure.exception.SessionNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@PreAuthorize("hasAnyRole('PROFESSOR','MANAGER','ADMIN','SUPERADMIN')")
public class UpdateSessionAlertService implements UpdateSessionAlertUseCase {

    private final ClassSessionRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateSessionAlertService(ClassSessionRepository repository,
                                     ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SessionActionResult execute(UpdateSessionAlertCommand cmd) {
        ClassSession session = repository
                .findByClassAndDate(cmd.tenantId(), cmd.classId(), cmd.sessionDate())
                .orElseThrow(() -> new SessionNotFoundException(cmd.classId(), cmd.sessionDate()));

        try {
            session.updateAlertReason(cmd.newReason(), cmd.actorId(), cmd.actorRole());
        } catch (AlertAuthorViolationException ex) {
            throw new NotAlertAuthorException();
        } catch (IllegalArgumentException ex) {
            throw new InvalidAlertReasonException(ex.getMessage());
        }

        repository.save(session);
        for (DomainEvent e : session.getDomainEvents()) eventPublisher.publishEvent(e);
        session.clearDomainEvents();

        return new SessionActionResult(
                session.getId().value(),
                session.getStatus().name(),
                session.getAlertReason(),
                session.getAlertedBy(),
                session.getUpdatedAt());
    }
}
