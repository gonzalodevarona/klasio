package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.RaiseSessionAlertCommand;
import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.domain.event.SessionAlertRaised;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.infrastructure.exception.SessionAlreadyStartedException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RaiseSessionAlertServiceTest {

    private static final String REASON = "thunderstorm flooded the courts";

    private final ClassDetailsPort classDetails = mock(ClassDetailsPort.class);
    private final ClassSessionRepository sessionRepo = mock(ClassSessionRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final RaiseSessionAlertService service =
            new RaiseSessionAlertService(classDetails, sessionRepo, publisher);

    @Test
    void professorAssignedToClassRaisesAlertSuccessfully() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), professorId)));

        when(sessionRepo.findOrCreate(eq(tenantId), eq(classId), eq(date), any(), any(), eq(professorId)))
                .thenReturn(sampleFutureSession(tenantId, classId, date, professorId));

        SessionActionResult r = service.execute(new RaiseSessionAlertCommand(
                tenantId, classId, date, REASON, professorId, null, "PROFESSOR"));

        assertThat(r.status()).isEqualTo("ALERTED");
        assertThat(r.reason()).isEqualTo(REASON);
        verify(publisher, atLeastOnce()).publishEvent(any(SessionAlertRaised.class));
    }

    @Test
    void professorNotAssignedToClassIs403() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID actingProf = UUID.randomUUID();
        UUID assignedProf = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), assignedProf)));

        assertThatThrownBy(() -> service.execute(new RaiseSessionAlertCommand(
                tenantId, classId, date, REASON, actingProf, null, "PROFESSOR")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void managerOutsideProgramIs403() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID classProgram = UUID.randomUUID();
        UUID managerProgram = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, classProgram, UUID.randomUUID())));

        assertThatThrownBy(() -> service.execute(new RaiseSessionAlertCommand(
                tenantId, classId, date, REASON, managerId, managerProgram, "MANAGER")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void adminCanAlertAnyClassInTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), UUID.randomUUID())));
        when(sessionRepo.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleFutureSession(tenantId, classId, date, adminId));

        SessionActionResult r = service.execute(new RaiseSessionAlertCommand(
                tenantId, classId, date, REASON, adminId, null, "ADMIN"));
        assertThat(r.status()).isEqualTo("ALERTED");
    }

    @Test
    void sessionAlreadyStartedIs409() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        LocalDate today = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), professorId)));

        // Session in the past: end time is 00:01 and session date is today — already started
        ClassSession past = ClassSession.materialize(tenantId, classId, today,
                LocalTime.of(0, 0), LocalTime.of(0, 1), professorId);
        when(sessionRepo.findOrCreate(any(), any(), any(), any(), any(), any())).thenReturn(past);

        assertThatThrownBy(() -> service.execute(new RaiseSessionAlertCommand(
                tenantId, classId, today, REASON, professorId, null, "PROFESSOR")))
                .isInstanceOf(SessionAlreadyStartedException.class);
    }

    private static ClassSession sampleFutureSession(UUID tenantId, UUID classId, LocalDate date, UUID actor) {
        return ClassSession.materialize(tenantId, classId, date,
                LocalTime.of(23, 0), LocalTime.of(23, 59), actor);
    }
}
