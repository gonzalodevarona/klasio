package com.klasio.attendance.infrastructure.notification;

import com.klasio.attendance.domain.event.SessionAlertRaised;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ProgramManagerPort;
import com.klasio.notifications.application.dto.CreateNotificationCommand;
import com.klasio.notifications.application.port.input.CreateNotificationUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SessionEventsNotificationListenerTest {

    private final ClassDetailsPort classDetails = mock(ClassDetailsPort.class);
    private final AttendanceRegistrationRepository regRepo = mock(AttendanceRegistrationRepository.class);
    private final ProgramManagerPort managerPort = mock(ProgramManagerPort.class);
    private final CreateNotificationUseCase createNotif = mock(CreateNotificationUseCase.class);

    private final SessionEventsNotificationListener listener =
            new SessionEventsNotificationListener(classDetails, regRepo, managerPort, createNotif);

    @Test
    void alertFromProfessorNotifiesStudentsAndManagerButNotTheProfessor() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UUID manager = UUID.randomUUID();

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(classId, programId, professorId)));
        when(classDetails.findClassName(tenantId, classId)).thenReturn(Optional.of("Hatha Yoga"));
        when(managerPort.findManagerUserIds(tenantId, programId)).thenReturn(Set.of(manager));

        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        when(regRepo.findAllNonCancelledBySessionId(tenantId, sessionId))
                .thenReturn(List.of(
                        regOfStudent(tenantId, s1, sessionId, classId),
                        regOfStudent(tenantId, s2, sessionId, classId)));

        listener.onSessionAlertRaised(new SessionAlertRaised(
                sessionId, tenantId, classId, "a perfectly long reason for alerting",
                professorId, "PROFESSOR", Instant.now()));

        ArgumentCaptor<CreateNotificationCommand> cap = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotif, times(3)).execute(cap.capture());

        Set<UUID> recipients = cap.getAllValues().stream()
                .map(CreateNotificationCommand::recipientUserId)
                .collect(Collectors.toSet());
        assertThat(recipients).containsExactlyInAnyOrder(s1, s2, manager);
        assertThat(recipients).doesNotContain(professorId);
    }

    @Test
    void cancellationFromAdminNotifiesStudentsAndProfessorAndManagerButNotTheAdmin() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UUID manager = UUID.randomUUID();
        UUID admin = UUID.randomUUID();

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(classId, programId, professorId)));
        when(classDetails.findClassName(tenantId, classId)).thenReturn(Optional.of("Hatha Yoga"));
        when(managerPort.findManagerUserIds(tenantId, programId)).thenReturn(Set.of(manager));

        UUID s1 = UUID.randomUUID();
        List<UUID> affected = List.of(s1);

        listener.onSessionCancelled(new SessionCancelled(
                sessionId, tenantId, classId, "the venue was flooded overnight",
                admin, "ADMIN", affected, Instant.now()));

        ArgumentCaptor<CreateNotificationCommand> cap = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotif, atLeastOnce()).execute(cap.capture());

        Set<UUID> recipients = cap.getAllValues().stream()
                .map(CreateNotificationCommand::recipientUserId)
                .collect(Collectors.toSet());
        assertThat(recipients).containsExactlyInAnyOrder(s1, professorId, manager);
        assertThat(recipients).doesNotContain(admin);
    }

    @Test
    void cancellationFromProfessorDoesNotNotifyTheProfessor() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UUID manager = UUID.randomUUID();

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(classId, programId, professorId)));
        when(classDetails.findClassName(tenantId, classId)).thenReturn(Optional.of("Hatha Yoga"));
        when(managerPort.findManagerUserIds(tenantId, programId)).thenReturn(Set.of(manager));

        listener.onSessionCancelled(new SessionCancelled(
                UUID.randomUUID(), tenantId, classId, "rain made the courts unusable today",
                professorId, "PROFESSOR", List.of(UUID.randomUUID()), Instant.now()));

        ArgumentCaptor<CreateNotificationCommand> cap = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotif, atLeastOnce()).execute(cap.capture());
        assertThat(cap.getAllValues().stream().map(CreateNotificationCommand::recipientUserId))
                .doesNotContain(professorId);
    }

    @Test
    void cancellationWithEmptyAffectedListSendsNoNotifications() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();

        listener.onSessionCancelled(new com.klasio.attendance.domain.event.SessionCancelled(
                UUID.randomUUID(), tenantId, classId, "some reason longer than twenty chars",
                UUID.randomUUID(), "ADMIN", List.of(), java.time.Instant.now()));

        verify(createNotif, never()).execute(any());
    }

    private static AttendanceRegistration regOfStudent(UUID tenantId, UUID studentId, UUID sessionId, UUID classId) {
        return AttendanceRegistration.register(sessionId, tenantId, classId,
                studentId, UUID.randomUUID(), UUID.randomUUID(),
                "BEGINNER", 1, 60,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                UUID.randomUUID());
    }
}
