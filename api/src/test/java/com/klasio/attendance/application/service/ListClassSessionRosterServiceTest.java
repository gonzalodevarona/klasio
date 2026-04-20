package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.ClassSessionRosterView;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationId;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.membership.domain.port.StudentNamePort;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListClassSessionRosterServiceTest {

    @Mock ClassDetailsPort classDetailsPort;
    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock ClassSessionRepository sessionRepository;
    @Mock StudentNamePort studentNamePort;
    @Mock ProfessorIdLookupPort professorIdLookupPort;

    @InjectMocks ListClassSessionRosterService service;

    private static final UUID TENANT_ID   = UUID.randomUUID();
    private static final UUID CLASS_ID    = UUID.randomUUID();
    private static final UUID PROGRAM_ID  = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID USER_ID     = UUID.randomUUID();
    private static final UUID STUDENT_ID  = UUID.randomUUID();

    private static final LocalDate FROM = LocalDate.now();
    private static final LocalDate TO   = FROM.plusDays(6);

    @BeforeEach
    void stubDefaults() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(new ClassSummaryView(CLASS_ID, PROGRAM_ID, PROFESSOR_ID)));
        // No ClassSession row by default — status falls back to SCHEDULED.
        when(sessionRepository.findByClassAndDate(any(), any(), any())).thenReturn(Optional.empty());
    }

    // ── Window validation ────────────────────────────────────────────────────

    @Test
    void windowExceeds30Days_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, FROM.plusDays(31),
                        "ADMIN", USER_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("30");

        verify(classDetailsPort, never()).findClassSummary(any(), any());
    }

    // ── Class not found ──────────────────────────────────────────────────────

    @Test
    void classNotFound_throwsClassNotFound() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null))
                .isInstanceOf(ClassNotFoundException.class);
    }

    // ── RBAC scope guards ────────────────────────────────────────────────────

    @Test
    void admin_passesThrough() {
        when(registrationRepository.findByClassAndDateRange(any(), any(), any(), any()))
                .thenReturn(List.of());

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).isEmpty();
    }

    @Test
    void superadmin_passesThrough() {
        when(registrationRepository.findByClassAndDateRange(any(), any(), any(), any()))
                .thenReturn(List.of());

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "SUPERADMIN", USER_ID, null);

        assertThat(result).isEmpty();
    }

    @Test
    void manager_correctProgram_passes() {
        when(registrationRepository.findByClassAndDateRange(any(), any(), any(), any()))
                .thenReturn(List.of());

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "MANAGER", USER_ID, PROGRAM_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void manager_wrongProgram_throwsAccessDenied() {
        UUID otherProgram = UUID.randomUUID();

        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "MANAGER", USER_ID, otherProgram))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("program");
    }

    @Test
    void manager_nullProgramIdInJwt_throwsAccessDenied() {
        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "MANAGER", USER_ID, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void professor_assignedToClass_passes() {
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(PROFESSOR_ID));
        when(registrationRepository.findByClassAndDateRange(any(), any(), any(), any()))
                .thenReturn(List.of());

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "PROFESSOR", USER_ID, null);

        assertThat(result).isEmpty();
    }

    @Test
    void professor_notAssignedToClass_throwsAccessDenied() {
        UUID otherProfessor = UUID.randomUUID();
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(otherProfessor));

        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "PROFESSOR", USER_ID, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("professor");
    }

    @Test
    void professor_noProfileFound_throwsAccessDenied() {
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "PROFESSOR", USER_ID, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("professor profile");
    }

    // ── Happy path — grouping and name enrichment ────────────────────────────

    @Test
    void happyPath_groupsRegistrantsBySession() {
        UUID regId1 = UUID.randomUUID();
        UUID regId2 = UUID.randomUUID();
        UUID student2 = UUID.randomUUID();

        LocalDate sessionDate = FROM.plusDays(1);
        LocalTime start = LocalTime.of(18, 0);
        LocalTime end   = LocalTime.of(19, 0);

        AttendanceRegistration reg1 = buildRegistration(regId1, STUDENT_ID, sessionDate, start, end, "REGISTERED");
        AttendanceRegistration reg2 = buildRegistration(regId2, student2,   sessionDate, start, end, "PRESENT");

        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(reg1, reg2));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("Juan Pérez"));
        when(studentNamePort.findFullName(student2,   TENANT_ID)).thenReturn(Optional.of("María López"));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).hasSize(1);
        ClassSessionRosterView session = result.get(0);
        assertThat(session.sessionDate()).isEqualTo(sessionDate);
        assertThat(session.startTime()).isEqualTo(start);
        assertThat(session.endTime()).isEqualTo(end);
        assertThat(session.registrants()).hasSize(2);
        assertThat(session.registrants()).extracting(ClassSessionRosterView.RegistrantView::studentName)
                .containsExactlyInAnyOrder("Juan Pérez", "María López");
    }

    @Test
    void twoSessions_sameDateDifferentTime_returnsTwoGroups() {
        UUID regMorning   = UUID.randomUUID();
        UUID regEvening   = UUID.randomUUID();
        LocalDate day     = FROM.plusDays(2);
        LocalTime morning = LocalTime.of(8, 0);
        LocalTime noon    = LocalTime.of(9, 0);
        LocalTime evening = LocalTime.of(18, 0);
        LocalTime night   = LocalTime.of(19, 0);

        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(
                        buildRegistration(regMorning, STUDENT_ID, day, morning, noon,    "REGISTERED"),
                        buildRegistration(regEvening, STUDENT_ID, day, evening, night,   "REGISTERED")
                ));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("Test User"));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void unknownStudentName_fallsBackToUnknown() {
        UUID regId = UUID.randomUUID();
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(buildRegistration(regId, STUDENT_ID,
                        FROM.plusDays(1), LocalTime.of(18, 0), LocalTime.of(19, 0), "REGISTERED")));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.empty());

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result.get(0).registrants().get(0).studentName()).isEqualTo("Unknown");
    }

    @Test
    void sessionStatus_defaultsToScheduledWhenNoSessionRow() {
        UUID regId = UUID.randomUUID();
        LocalDate day = FROM.plusDays(1);
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(buildRegistration(regId, STUDENT_ID,
                        day, LocalTime.of(18, 0), LocalTime.of(19, 0), "REGISTERED")));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("Test User"));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result.get(0).status()).isEqualTo("SCHEDULED");
        assertThat(result.get(0).alertReason()).isNull();
        assertThat(result.get(0).cancellationReason()).isNull();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private AttendanceRegistration buildRegistration(UUID regId, UUID studentId,
                                                      LocalDate sessionDate, LocalTime start,
                                                      LocalTime end, String status) {
        return AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(regId),
                TENANT_ID,
                UUID.randomUUID(), // sessionId
                CLASS_ID,
                studentId,
                UUID.randomUUID(), // enrollmentId
                UUID.randomUUID(), // membershipId
                "BEGINNER",
                1,
                AttendanceRegistrationStatus.valueOf(status),
                sessionDate,
                start,
                end,
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                null, null,         // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), USER_ID,
                null, null
        );
    }
}
