package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.dto.UpdateSessionAlertCommand;
import com.klasio.attendance.domain.event.SessionAlertUpdated;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionId;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.infrastructure.exception.NotAlertAuthorException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateSessionAlertServiceTest {

    private static final String NEW_REASON = "court is still unplayable due to storm";
    private final ClassSessionRepository repo = mock(ClassSessionRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final UpdateSessionAlertService service = new UpdateSessionAlertService(repo, publisher);

    @Test
    void authorUpdatesReasonSuccessfully() {
        UUID tenantId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        ClassSession s = alertedSession(tenantId, author);
        when(repo.findById(tenantId, s.getId().value())).thenReturn(Optional.of(s));

        SessionActionResult r = service.execute(new UpdateSessionAlertCommand(
                tenantId, s.getId().value(), NEW_REASON, author, "PROFESSOR"));

        assertThat(r.reason()).isEqualTo(NEW_REASON);
        verify(publisher).publishEvent(any(SessionAlertUpdated.class));
        verify(repo).save(s);
    }

    @Test
    void nonAuthorIs403() {
        UUID tenantId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        ClassSession s = alertedSession(tenantId, author);
        when(repo.findById(tenantId, s.getId().value())).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.execute(new UpdateSessionAlertCommand(
                tenantId, s.getId().value(), NEW_REASON, UUID.randomUUID(), "PROFESSOR")))
                .isInstanceOf(NotAlertAuthorException.class);
    }

    private static ClassSession alertedSession(UUID tenantId, UUID author) {
        ClassSession s = ClassSession.materialize(tenantId, UUID.randomUUID(),
                LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), author);
        s.raiseAlert("initial rain warning for outdoor court", author, "PROFESSOR");
        s.clearDomainEvents();
        return s;
    }
}
