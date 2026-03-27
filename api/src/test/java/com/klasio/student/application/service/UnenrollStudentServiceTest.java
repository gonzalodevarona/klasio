package com.klasio.student.application.service;

import com.klasio.shared.infrastructure.exception.EnrollmentAlreadyInactiveException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.student.application.dto.UnenrollStudentCommand;
import com.klasio.student.domain.event.StudentUnenrolled;
import com.klasio.student.domain.model.Level;
import com.klasio.student.domain.model.LevelHistoryEntry;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.model.StudentEnrollmentId;
import com.klasio.student.domain.port.LevelHistoryRepository;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnenrollStudentServiceTest {

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @Mock
    private LevelHistoryRepository levelHistoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UnenrollStudentService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CHANGED_BY = UUID.randomUUID();
    private static final String ROLE = "ADMIN";
    private static final Level LEVEL = Level.BEGINNER;

    @BeforeEach
    void setUp() {
        service = new UnenrollStudentService(enrollmentRepository, levelHistoryRepository, eventPublisher);
    }

    private StudentEnrollment createActiveEnrollment() {
        return StudentEnrollment.reconstitute(
                StudentEnrollmentId.of(ENROLLMENT_ID),
                TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL,
                LocalDate.now(), "ACTIVE", Instant.now(), CHANGED_BY, null, null);
    }

    private StudentEnrollment createInactiveEnrollment() {
        return StudentEnrollment.reconstitute(
                StudentEnrollmentId.of(ENROLLMENT_ID),
                TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL,
                LocalDate.now(), "INACTIVE", Instant.now(), CHANGED_BY, Instant.now(), CHANGED_BY);
    }

    @Test
    @DisplayName("should deactivate enrollment, save unenrollment history entry, and publish event")
    void happyPath_unenrollsStudentAndPublishesEvent() {
        when(enrollmentRepository.findById(TENANT_ID, ENROLLMENT_ID))
                .thenReturn(Optional.of(createActiveEnrollment()));

        UnenrollStudentCommand command = new UnenrollStudentCommand(
                TENANT_ID, ENROLLMENT_ID, CHANGED_BY, ROLE);

        StudentEnrollment result = service.execute(command);

        assertThat(result.getStatus()).isEqualTo("INACTIVE");

        ArgumentCaptor<StudentEnrollment> enrollmentCaptor = ArgumentCaptor.forClass(StudentEnrollment.class);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        assertThat(enrollmentCaptor.getValue().getStatus()).isEqualTo("INACTIVE");

        ArgumentCaptor<LevelHistoryEntry> historyCaptor = ArgumentCaptor.forClass(LevelHistoryEntry.class);
        verify(levelHistoryRepository).save(historyCaptor.capture());
        LevelHistoryEntry entry = historyCaptor.getValue();
        assertThat(entry.getAction()).isEqualTo(LevelHistoryEntry.Action.UNENROLLED);
        assertThat(entry.getPreviousLevel()).isEqualTo(LEVEL);
        assertThat(entry.getNewLevel()).isNull();

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(StudentUnenrolled.class);
        StudentUnenrolled event = (StudentUnenrolled) eventCaptor.getValue();
        assertThat(event.enrollmentId()).isEqualTo(ENROLLMENT_ID);
        assertThat(event.level()).isEqualTo(LEVEL.name());
    }

    @Test
    @DisplayName("should throw EnrollmentNotFoundException when enrollment does not exist")
    void notFound_throwsEnrollmentNotFoundException() {
        when(enrollmentRepository.findById(TENANT_ID, ENROLLMENT_ID))
                .thenReturn(Optional.empty());

        UnenrollStudentCommand command = new UnenrollStudentCommand(
                TENANT_ID, ENROLLMENT_ID, CHANGED_BY, ROLE);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(EnrollmentNotFoundException.class)
                .hasMessageContaining(ENROLLMENT_ID.toString());

        verify(enrollmentRepository, never()).save(any());
        verify(levelHistoryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw EnrollmentAlreadyInactiveException when enrollment is already inactive")
    void alreadyInactive_throwsEnrollmentAlreadyInactiveException() {
        when(enrollmentRepository.findById(TENANT_ID, ENROLLMENT_ID))
                .thenReturn(Optional.of(createInactiveEnrollment()));

        UnenrollStudentCommand command = new UnenrollStudentCommand(
                TENANT_ID, ENROLLMENT_ID, CHANGED_BY, ROLE);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(EnrollmentAlreadyInactiveException.class)
                .hasMessageContaining(ENROLLMENT_ID.toString());

        verify(enrollmentRepository, never()).save(any());
        verify(levelHistoryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
