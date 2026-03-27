package com.klasio.student.application.service;

import com.klasio.shared.infrastructure.exception.EnrollmentAlreadyInactiveException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.student.application.dto.PromoteStudentCommand;
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
class PromoteStudentServiceTest {

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @Mock
    private LevelHistoryRepository levelHistoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PromoteStudentService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID SOURCE_ENROLLMENT_ID = UUID.randomUUID();
    private static final UUID TARGET_ENROLLMENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CHANGED_BY = UUID.randomUUID();
    private static final String ROLE = "ADMIN";

    @BeforeEach
    void setUp() {
        service = new PromoteStudentService(enrollmentRepository, levelHistoryRepository, eventPublisher);
    }

    private StudentEnrollment createActiveEnrollment(UUID enrollmentId, Level level) {
        return StudentEnrollment.reconstitute(
                StudentEnrollmentId.of(enrollmentId),
                TENANT_ID, STUDENT_ID, PROGRAM_ID, level,
                LocalDate.now(), "ACTIVE", Instant.now(), CHANGED_BY, null, null);
    }

    private StudentEnrollment createInactiveEnrollment(UUID enrollmentId, Level level) {
        return StudentEnrollment.reconstitute(
                StudentEnrollmentId.of(enrollmentId),
                TENANT_ID, STUDENT_ID, PROGRAM_ID, level,
                LocalDate.now(), "INACTIVE", Instant.now(), CHANGED_BY, Instant.now(), CHANGED_BY);
    }

    @Test
    @DisplayName("should deactivate source, create new enrollment at target level, and record promotion history when no existing target enrollment")
    void noExistingTarget_createsNewEnrollmentAndRecordsHistory() {
        StudentEnrollment source = createActiveEnrollment(SOURCE_ENROLLMENT_ID, Level.BEGINNER);
        when(enrollmentRepository.findById(TENANT_ID, SOURCE_ENROLLMENT_ID))
                .thenReturn(Optional.of(source));
        when(enrollmentRepository.findActiveByStudentIdAndProgramIdAndLevel(
                TENANT_ID, STUDENT_ID, PROGRAM_ID, Level.INTERMEDIATE.name()))
                .thenReturn(Optional.empty());

        PromoteStudentCommand command = new PromoteStudentCommand(
                TENANT_ID, SOURCE_ENROLLMENT_ID, Level.INTERMEDIATE, CHANGED_BY, ROLE);

        StudentEnrollment result = service.execute(command);

        assertThat(result.getLevel()).isEqualTo(Level.INTERMEDIATE);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        // Source should be saved as INACTIVE
        ArgumentCaptor<StudentEnrollment> enrollmentCaptor = ArgumentCaptor.forClass(StudentEnrollment.class);
        verify(enrollmentRepository, org.mockito.Mockito.times(2)).save(enrollmentCaptor.capture());
        assertThat(enrollmentCaptor.getAllValues().get(0).getStatus()).isEqualTo("INACTIVE");
        assertThat(enrollmentCaptor.getAllValues().get(1).getLevel()).isEqualTo(Level.INTERMEDIATE);

        ArgumentCaptor<LevelHistoryEntry> historyCaptor = ArgumentCaptor.forClass(LevelHistoryEntry.class);
        verify(levelHistoryRepository).save(historyCaptor.capture());
        LevelHistoryEntry entry = historyCaptor.getValue();
        assertThat(entry.getAction()).isEqualTo(LevelHistoryEntry.Action.PROMOTED);
        assertThat(entry.getPreviousLevel()).isEqualTo(Level.BEGINNER);
        assertThat(entry.getNewLevel()).isEqualTo(Level.INTERMEDIATE);
    }

    @Test
    @DisplayName("should deactivate source and record promotion history on existing target enrollment")
    void existingTarget_deactivatesSourceAndAddsHistoryToTarget() {
        StudentEnrollment source = createActiveEnrollment(SOURCE_ENROLLMENT_ID, Level.BEGINNER);
        StudentEnrollment target = createActiveEnrollment(TARGET_ENROLLMENT_ID, Level.INTERMEDIATE);

        when(enrollmentRepository.findById(TENANT_ID, SOURCE_ENROLLMENT_ID))
                .thenReturn(Optional.of(source));
        when(enrollmentRepository.findActiveByStudentIdAndProgramIdAndLevel(
                TENANT_ID, STUDENT_ID, PROGRAM_ID, Level.INTERMEDIATE.name()))
                .thenReturn(Optional.of(target));

        PromoteStudentCommand command = new PromoteStudentCommand(
                TENANT_ID, SOURCE_ENROLLMENT_ID, Level.INTERMEDIATE, CHANGED_BY, ROLE);

        StudentEnrollment result = service.execute(command);

        assertThat(result.getId().value()).isEqualTo(TARGET_ENROLLMENT_ID);
        assertThat(result.getLevel()).isEqualTo(Level.INTERMEDIATE);

        // Only source saved (target not modified, only history added)
        ArgumentCaptor<StudentEnrollment> enrollmentCaptor = ArgumentCaptor.forClass(StudentEnrollment.class);
        verify(enrollmentRepository, org.mockito.Mockito.times(1)).save(enrollmentCaptor.capture());
        assertThat(enrollmentCaptor.getValue().getStatus()).isEqualTo("INACTIVE");

        ArgumentCaptor<LevelHistoryEntry> historyCaptor = ArgumentCaptor.forClass(LevelHistoryEntry.class);
        verify(levelHistoryRepository).save(historyCaptor.capture());
        LevelHistoryEntry entry = historyCaptor.getValue();
        assertThat(entry.getAction()).isEqualTo(LevelHistoryEntry.Action.PROMOTED);
        assertThat(entry.getPreviousLevel()).isEqualTo(Level.BEGINNER);
        assertThat(entry.getNewLevel()).isEqualTo(Level.INTERMEDIATE);
        assertThat(entry.getEnrollmentId()).isEqualTo(TARGET_ENROLLMENT_ID);
    }

    @Test
    @DisplayName("should throw EnrollmentNotFoundException when source enrollment does not exist")
    void notFound_throwsEnrollmentNotFoundException() {
        when(enrollmentRepository.findById(TENANT_ID, SOURCE_ENROLLMENT_ID))
                .thenReturn(Optional.empty());

        PromoteStudentCommand command = new PromoteStudentCommand(
                TENANT_ID, SOURCE_ENROLLMENT_ID, Level.INTERMEDIATE, CHANGED_BY, ROLE);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(EnrollmentNotFoundException.class)
                .hasMessageContaining(SOURCE_ENROLLMENT_ID.toString());

        verify(enrollmentRepository, never()).save(any());
        verify(levelHistoryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw EnrollmentAlreadyInactiveException when source is inactive")
    void alreadyInactive_throwsEnrollmentAlreadyInactiveException() {
        when(enrollmentRepository.findById(TENANT_ID, SOURCE_ENROLLMENT_ID))
                .thenReturn(Optional.of(createInactiveEnrollment(SOURCE_ENROLLMENT_ID, Level.BEGINNER)));

        PromoteStudentCommand command = new PromoteStudentCommand(
                TENANT_ID, SOURCE_ENROLLMENT_ID, Level.INTERMEDIATE, CHANGED_BY, ROLE);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(EnrollmentAlreadyInactiveException.class)
                .hasMessageContaining(SOURCE_ENROLLMENT_ID.toString());

        verify(enrollmentRepository, never()).save(any());
        verify(levelHistoryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when target level equals current level")
    void sameLevel_throwsIllegalArgumentException() {
        when(enrollmentRepository.findById(TENANT_ID, SOURCE_ENROLLMENT_ID))
                .thenReturn(Optional.of(createActiveEnrollment(SOURCE_ENROLLMENT_ID, Level.BEGINNER)));

        PromoteStudentCommand command = new PromoteStudentCommand(
                TENANT_ID, SOURCE_ENROLLMENT_ID, Level.BEGINNER, CHANGED_BY, ROLE);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BEGINNER");

        verify(enrollmentRepository, never()).save(any());
        verify(levelHistoryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
