package com.klasio.attendance.infrastructure.notification;

import com.klasio.attendance.domain.event.SessionAlertRaised;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ProfessorUserIdPort;
import com.klasio.attendance.domain.port.ProgramManagerPort;
import com.klasio.attendance.domain.port.StudentEmailPort;
import com.klasio.attendance.domain.port.StudentUserIdPort;
import com.klasio.email.application.EmailService;
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

    private static final LocalDate SESSION_DATE = LocalDate.now().plusDays(1);
    private static final LocalTime START = LocalTime.of(10, 0);
    private static final LocalTime END   = LocalTime.of(11, 0);

    private final ClassDetailsPort classDetails   = mock(ClassDetailsPort.class);
    private final AttendanceRegistrationRepository regRepo = mock(AttendanceRegistrationRepository.class);
    private final ProgramManagerPort managerPort  = mock(ProgramManagerPort.class);
    private final CreateNotificationUseCase createNotif = mock(CreateNotificationUseCase.class);
    private final StudentUserIdPort studentUserIdPort  = mock(StudentUserIdPort.class);
    private final ProfessorUserIdPort professorUserIdPort = mock(ProfessorUserIdPort.class);
    private final EmailService emailService = mock(EmailService.class);
    private final StudentEmailPort studentEmailPort = mock(StudentEmailPort.class);

    private final SessionEventsNotificationListener listener =
            new SessionEventsNotificationListener(
                    classDetails, regRepo, managerPort, createNotif,
                    studentUserIdPort, professorUserIdPort,
                    emailService, studentEmailPort);

    @Test
    void alertFromProfessorNotifiesStudentsAndManagerButNotTheProfessor() {
        UUID tenantId   = UUID.randomUUID();
        UUID classId    = UUID.randomUUID();
        UUID sessionId  = UUID.randomUUID();
        UUID programId  = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UUID professorUserId = UUID.randomUUID();
        UUID manager    = UUID.randomUUID();

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(classId, programId, professorId)));
        when(classDetails.findClassName(tenantId, classId)).thenReturn(Optional.of("Hatha Yoga"));
        when(managerPort.findManagerUserIds(tenantId, programId)).thenReturn(Set.of(manager));
        when(professorUserIdPort.findUserIdByProfessorId(tenantId, professorId))
                .thenReturn(Optional.of(professorUserId));

        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID s1UserId = UUID.randomUUID();
        UUID s2UserId = UUID.randomUUID();

        AttendanceRegistration reg1 = regOfStudent(tenantId, s1, sessionId, classId);
        AttendanceRegistration reg2 = regOfStudent(tenantId, s2, sessionId, classId);
        when(regRepo.findAllNonCancelledBySessionId(tenantId, sessionId)).thenReturn(List.of(reg1, reg2));
        when(studentUserIdPort.findUserIdByStudentId(tenantId, s1)).thenReturn(Optional.of(s1UserId));
        when(studentUserIdPort.findUserIdByStudentId(tenantId, s2)).thenReturn(Optional.of(s2UserId));

        // professorId is the actor — actor comparison uses professorUserId after resolution
        listener.onSessionAlertRaised(new SessionAlertRaised(
                sessionId, tenantId, classId, "a perfectly long reason for alerting",
                SESSION_DATE, START, END,
                professorUserId, "PROFESSOR", Instant.now()));

        ArgumentCaptor<CreateNotificationCommand> cap = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotif, times(3)).execute(cap.capture());

        Set<UUID> recipients = cap.getAllValues().stream()
                .map(CreateNotificationCommand::recipientUserId)
                .collect(Collectors.toSet());
        assertThat(recipients).containsExactlyInAnyOrder(s1UserId, s2UserId, manager);
        assertThat(recipients).doesNotContain(professorUserId);
    }

    @Test
    void cancellationFromAdminNotifiesStudentsAndProfessorAndManagerButNotTheAdmin() {
        UUID tenantId   = UUID.randomUUID();
        UUID classId    = UUID.randomUUID();
        UUID sessionId  = UUID.randomUUID();
        UUID programId  = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UUID professorUserId = UUID.randomUUID();
        UUID manager    = UUID.randomUUID();
        UUID admin      = UUID.randomUUID();

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(classId, programId, professorId)));
        when(classDetails.findClassName(tenantId, classId)).thenReturn(Optional.of("Hatha Yoga"));
        when(managerPort.findManagerUserIds(tenantId, programId)).thenReturn(Set.of(manager));
        when(professorUserIdPort.findUserIdByProfessorId(tenantId, professorId))
                .thenReturn(Optional.of(professorUserId));

        UUID s1 = UUID.randomUUID();
        UUID s1UserId = UUID.randomUUID();
        when(studentUserIdPort.findUserIdByStudentId(tenantId, s1)).thenReturn(Optional.of(s1UserId));

        listener.onSessionCancelled(new SessionCancelled(
                sessionId, tenantId, classId, "the venue was flooded overnight",
                SESSION_DATE, START, END,
                admin, "ADMIN", List.of(s1), Instant.now()));

        ArgumentCaptor<CreateNotificationCommand> cap = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotif, atLeastOnce()).execute(cap.capture());

        Set<UUID> recipients = cap.getAllValues().stream()
                .map(CreateNotificationCommand::recipientUserId)
                .collect(Collectors.toSet());
        assertThat(recipients).containsExactlyInAnyOrder(s1UserId, professorUserId, manager);
        assertThat(recipients).doesNotContain(admin);
    }

    @Test
    void cancellationFromProfessorDoesNotNotifyTheProfessor() {
        UUID tenantId   = UUID.randomUUID();
        UUID classId    = UUID.randomUUID();
        UUID programId  = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UUID professorUserId = UUID.randomUUID();
        UUID manager    = UUID.randomUUID();
        UUID s1         = UUID.randomUUID();
        UUID s1UserId   = UUID.randomUUID();

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(classId, programId, professorId)));
        when(classDetails.findClassName(tenantId, classId)).thenReturn(Optional.of("Hatha Yoga"));
        when(managerPort.findManagerUserIds(tenantId, programId)).thenReturn(Set.of(manager));
        when(professorUserIdPort.findUserIdByProfessorId(tenantId, professorId))
                .thenReturn(Optional.of(professorUserId));
        when(studentUserIdPort.findUserIdByStudentId(tenantId, s1)).thenReturn(Optional.of(s1UserId));

        listener.onSessionCancelled(new SessionCancelled(
                UUID.randomUUID(), tenantId, classId, "rain made the courts unusable today",
                SESSION_DATE, START, END,
                professorUserId, "PROFESSOR", List.of(s1), Instant.now()));

        ArgumentCaptor<CreateNotificationCommand> cap = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotif, atLeastOnce()).execute(cap.capture());
        assertThat(cap.getAllValues().stream().map(CreateNotificationCommand::recipientUserId))
                .doesNotContain(professorUserId);
    }

    @Test
    void cancellationWithEmptyAffectedListSendsNoNotifications() {
        UUID tenantId = UUID.randomUUID();
        UUID classId  = UUID.randomUUID();

        listener.onSessionCancelled(new SessionCancelled(
                UUID.randomUUID(), tenantId, classId, "some reason longer than twenty chars",
                SESSION_DATE, START, END,
                UUID.randomUUID(), "ADMIN", List.of(), Instant.now()));

        verify(createNotif, never()).execute(any());
    }

    private static AttendanceRegistration regOfStudent(UUID tenantId, UUID studentId, UUID sessionId, UUID classId) {
        return AttendanceRegistration.register(sessionId, tenantId, classId,
                studentId, UUID.randomUUID(), UUID.randomUUID(),
                "BEGINNER", 1, 60,
                SESSION_DATE, START, END,
                UUID.randomUUID());
    }
}
