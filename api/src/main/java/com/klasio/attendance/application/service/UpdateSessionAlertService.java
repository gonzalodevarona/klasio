package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.dto.UpdateSessionAlertCommand;
import com.klasio.attendance.application.port.input.UpdateSessionAlertUseCase;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.InvalidAlertReasonException;
import com.klasio.shared.infrastructure.exception.NotAlertAuthorException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

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
                .findById(cmd.tenantId(), cmd.sessionId())
                .orElseThrow(() -> new NoSuchElementException("session not found: " + cmd.sessionId()));

        try {
            session.updateAlertReason(cmd.newReason(), cmd.actorId(), cmd.actorRole());
        } catch (IllegalArgumentException ex) {
            throw new InvalidAlertReasonException(ex.getMessage());
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("author")) {
                throw new NotAlertAuthorException();
            }
            throw ex;
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
