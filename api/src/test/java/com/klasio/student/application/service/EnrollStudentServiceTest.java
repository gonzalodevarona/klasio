package com.klasio.student.application.service;

import com.klasio.shared.infrastructure.exception.EnrollmentAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.StudentNotFoundException;
import com.klasio.student.application.dto.EnrollStudentCommand;
import com.klasio.student.domain.event.StudentEnrolled;
import com.klasio.student.domain.model.Level;
import com.klasio.student.domain.model.LevelHistoryEntry;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.port.LevelHistoryRepository;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import com.klasio.student.domain.port.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollStudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @Mock
    private LevelHistoryRepository levelHistoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private EnrollStudentService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();
    private static final Level LEVEL = Level.BEGINNER;
    private static final String ROLE = "ADMIN";

    @BeforeEach
    void setUp() {
        service = new EnrollStudentService(studentRepository, enrollmentRepository,
                levelHistoryRepository, eventPublisher);
    }

    private Student createActiveStudent() {
        return Student.reconstitute(
                com.klasio.student.domain.model.StudentId.of(STUDENT_ID),
                TENANT_ID, "Carlos", "Garcia", "carlos@example.com",
                java.time.LocalDate.of(2000, 1, 15), "Sura", "1234567890",
                com.klasio.shared.domain.model.IdentityDocumentType.CC,
                com.klasio.student.domain.model.BloodType.O_POSITIVE, "3001234567",
                null, null, null, null, null,
                "ACTIVE", java.time.Instant.now(), CREATED_BY,
                null, null, null, null);
    }

    private Student createInactiveStudent() {
        return Student.reconstitute(
                com.klasio.student.domain.model.StudentId.of(STUDENT_ID),
                TENANT_ID, "Carlos", "Garcia", "carlos@example.com",
                java.time.LocalDate.of(2000, 1, 15), "Sura", "1234567890",
                com.klasio.shared.domain.model.IdentityDocumentType.CC,
                com.klasio.student.domain.model.BloodType.O_POSITIVE, "3001234567",
                null, null, null, null, null,
                "INACTIVE", java.time.Instant.now(), CREATED_BY,
                null, null, java.time.Instant.now(), CREATED_BY);
    }

    @Test
    @DisplayName("should create enrollment, save history entry, and publish events")
    void happyPath_createsEnrollmentAndPublishesEvents() {
        when(studentRepository.findById(TENANT_ID, STUDENT_ID))
                .thenReturn(Optional.of(createActiveStudent()));
        when(enrollmentRepository.existsByStudentIdAndProgramIdAndLevelActive(STUDENT_ID, PROGRAM_ID, LEVEL.name()))
                .thenReturn(false);

        EnrollStudentCommand command = new EnrollStudentCommand(
                TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY, ROLE);

        StudentEnrollment result = service.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getLevel()).isEqualTo(LEVEL);
        assertThat(result.getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(result.getProgramId()).isEqualTo(PROGRAM_ID);

        ArgumentCaptor<StudentEnrollment> enrollmentCaptor = ArgumentCaptor.forClass(StudentEnrollment.class);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        assertThat(enrollmentCaptor.getValue().getStudentId()).isEqualTo(STUDENT_ID);

        ArgumentCaptor<LevelHistoryEntry> historyCaptor = ArgumentCaptor.forClass(LevelHistoryEntry.class);
        verify(levelHistoryRepository).save(historyCaptor.capture());
        LevelHistoryEntry savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getPreviousLevel()).isNull();
        assertThat(savedHistory.getNewLevel()).isEqualTo(LEVEL);
        assertThat(savedHistory.getChangedBy()).isEqualTo(CREATED_BY);
        assertThat(savedHistory.getChangedByRole()).isEqualTo(ROLE);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(StudentEnrolled.class);

        StudentEnrolled event = (StudentEnrolled) eventCaptor.getValue();
        assertThat(event.studentId()).isEqualTo(STUDENT_ID);
        assertThat(event.programId()).isEqualTo(PROGRAM_ID);
        assertThat(event.level()).isEqualTo(LEVEL.name());
    }

    @Test
    @DisplayName("should throw StudentNotFoundException when student does not exist")
    void studentNotFound_throwsStudentNotFoundException() {
        when(studentRepository.findById(TENANT_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        EnrollStudentCommand command = new EnrollStudentCommand(
                TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY, ROLE);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining(STUDENT_ID.toString());

        verify(enrollmentRepository, never()).save(any());
        verify(levelHistoryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw EnrollmentAlreadyExistsException when active enrollment exists for student+program+level")
    void duplicateEnrollment_throwsEnrollmentAlreadyExistsException() {
        when(studentRepository.findById(TENANT_ID, STUDENT_ID))
                .thenReturn(Optional.of(createActiveStudent()));
        when(enrollmentRepository.existsByStudentIdAndProgramIdAndLevelActive(STUDENT_ID, PROGRAM_ID, LEVEL.name()))
                .thenReturn(true);

        EnrollStudentCommand command = new EnrollStudentCommand(
                TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY, ROLE);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(EnrollmentAlreadyExistsException.class)
                .hasMessageContaining(STUDENT_ID.toString())
                .hasMessageContaining(PROGRAM_ID.toString());

        verify(enrollmentRepository, never()).save(any());
        verify(levelHistoryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should allow enrollment in the same program at a different level")
    void differentLevel_sameProgramAllowed() {
        Level differentLevel = Level.INTERMEDIATE;
        when(studentRepository.findById(TENANT_ID, STUDENT_ID))
                .thenReturn(Optional.of(createActiveStudent()));
        when(enrollmentRepository.existsByStudentIdAndProgramIdAndLevelActive(STUDENT_ID, PROGRAM_ID, differentLevel.name()))
                .thenReturn(false);

        EnrollStudentCommand command = new EnrollStudentCommand(
                TENANT_ID, STUDENT_ID, PROGRAM_ID, differentLevel, CREATED_BY, ROLE);

        StudentEnrollment result = service.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getLevel()).isEqualTo(differentLevel);
        verify(enrollmentRepository).save(any());
    }

    @Test
    @DisplayName("should throw StudentNotFoundException when student is inactive")
    void inactiveStudent_throwsStudentNotFoundException() {
        when(studentRepository.findById(TENANT_ID, STUDENT_ID))
                .thenReturn(Optional.of(createInactiveStudent()));

        EnrollStudentCommand command = new EnrollStudentCommand(
                TENANT_ID, STUDENT_ID, PROGRAM_ID, LEVEL, CREATED_BY, ROLE);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining("not active");

        verify(enrollmentRepository, never()).save(any());
        verify(levelHistoryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
