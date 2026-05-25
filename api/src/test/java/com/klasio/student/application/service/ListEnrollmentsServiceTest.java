package com.klasio.student.application.service;

import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.model.ProgramId;
import com.klasio.program.domain.model.ProgramStatus;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.student.application.dto.EnrollmentSummary;
import com.klasio.student.domain.model.BloodType;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.student.domain.model.Level;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.model.StudentEnrollmentId;
import com.klasio.student.domain.model.StudentId;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import com.klasio.student.domain.port.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEnrollmentsServiceTest {

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private StudentRepository studentRepository;

    private ListEnrollmentsService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ListEnrollmentsService(enrollmentRepository, programRepository, studentRepository);
    }

    private StudentEnrollment createEnrollment(UUID studentId, UUID programId, Level level) {
        return StudentEnrollment.reconstitute(
                StudentEnrollmentId.generate(), TENANT_ID, studentId, programId,
                level, LocalDate.now(), "ACTIVE", Instant.now(), UUID.randomUUID(),
                null, null);
    }

    private Program createProgram(UUID programId, String name) {
        return Program.reconstitute(ProgramId.of(programId), TENANT_ID, name, ProgramStatus.ACTIVE,
                null, Instant.now(), UUID.randomUUID(), null, null);
    }

    private Student createStudent(UUID studentId, String firstName, String lastName) {
        return Student.reconstitute(
                StudentId.of(studentId), TENANT_ID, null, firstName, lastName,
                firstName.toLowerCase() + "@example.com",
                LocalDate.of(1990, 1, 1), "Sura", "1234567890",
                IdentityDocumentType.CC, BloodType.O_POSITIVE, null,
                null, null, null, null, null,
                "ACTIVE", Instant.now(), UUID.randomUUID(), null, null, null, null);
    }

    @Nested
    @DisplayName("byProgram()")
    class ByProgram {

        @Test
        @DisplayName("should return paginated enrollments with student names resolved")
        void shouldReturnPaginatedEnrollmentsByProgram() {
            UUID studentId1 = UUID.randomUUID();
            UUID studentId2 = UUID.randomUUID();
            StudentEnrollment enrollment1 = createEnrollment(studentId1, PROGRAM_ID, Level.BEGINNER);
            StudentEnrollment enrollment2 = createEnrollment(studentId2, PROGRAM_ID, Level.ADVANCED);

            Page<StudentEnrollment> page = new PageImpl<>(List.of(enrollment1, enrollment2));
            when(enrollmentRepository.findByProgramId(TENANT_ID, PROGRAM_ID, 0, 10, null, null))
                    .thenReturn(page);
            when(studentRepository.findById(eq(TENANT_ID), any()))
                    .thenReturn(Optional.of(createStudent(studentId1, "Ana", "Lopez")));

            Page<EnrollmentSummary> result = service.byProgram(TENANT_ID, PROGRAM_ID, 0, 10, null, null);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).programId()).isEqualTo(PROGRAM_ID);
            assertThat(result.getContent().get(0).level()).isEqualTo("BEGINNER");
            assertThat(result.getContent().get(0).studentName()).isEqualTo("Ana Lopez");
            assertThat(result.getContent().get(1).level()).isEqualTo("ADVANCED");

            verify(enrollmentRepository).findByProgramId(TENANT_ID, PROGRAM_ID, 0, 10, null, null);
        }

        @Test
        @DisplayName("should return empty page when no enrollments found for program")
        void shouldReturnEmptyPageForProgram() {
            Page<StudentEnrollment> emptyPage = new PageImpl<>(List.of());
            when(enrollmentRepository.findByProgramId(TENANT_ID, PROGRAM_ID, 0, 10, "BEGINNER", "ACTIVE"))
                    .thenReturn(emptyPage);

            Page<EnrollmentSummary> result = service.byProgram(TENANT_ID, PROGRAM_ID, 0, 10, "BEGINNER", "ACTIVE");

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("byStudent()")
    class ByStudent {

        @Test
        @DisplayName("should return paginated enrollments with program names resolved")
        void shouldReturnPaginatedEnrollmentsByStudent() {
            UUID programId1 = UUID.randomUUID();
            UUID programId2 = UUID.randomUUID();
            StudentEnrollment enrollment1 = createEnrollment(STUDENT_ID, programId1, Level.BEGINNER);
            StudentEnrollment enrollment2 = createEnrollment(STUDENT_ID, programId2, Level.INTERMEDIATE);

            Page<StudentEnrollment> page = new PageImpl<>(List.of(enrollment1, enrollment2));
            when(enrollmentRepository.findByStudentId(TENANT_ID, STUDENT_ID, 0, 10, null))
                    .thenReturn(page);
            when(programRepository.findById(eq(TENANT_ID), eq(programId1)))
                    .thenReturn(Optional.of(createProgram(programId1, "Kids Program")));
            when(programRepository.findById(eq(TENANT_ID), eq(programId2)))
                    .thenReturn(Optional.of(createProgram(programId2, "Adult Program")));

            Page<EnrollmentSummary> result = service.byStudent(TENANT_ID, STUDENT_ID, 0, 10, null);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).studentId()).isEqualTo(STUDENT_ID);
            assertThat(result.getContent().get(0).programName()).isEqualTo("Kids Program");
            assertThat(result.getContent().get(1).programName()).isEqualTo("Adult Program");

            verify(enrollmentRepository).findByStudentId(TENANT_ID, STUDENT_ID, 0, 10, null);
        }

        @Test
        @DisplayName("should return null programName when program is not found")
        void shouldReturnNullProgramNameWhenNotFound() {
            UUID programId = UUID.randomUUID();
            StudentEnrollment enrollment = createEnrollment(STUDENT_ID, programId, Level.BEGINNER);

            Page<StudentEnrollment> page = new PageImpl<>(List.of(enrollment));
            when(enrollmentRepository.findByStudentId(TENANT_ID, STUDENT_ID, 0, 10, null))
                    .thenReturn(page);
            when(programRepository.findById(TENANT_ID, programId))
                    .thenReturn(Optional.empty());

            Page<EnrollmentSummary> result = service.byStudent(TENANT_ID, STUDENT_ID, 0, 10, null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).programName()).isNull();
        }

        @Test
        @DisplayName("should return empty page when no enrollments found for student")
        void shouldReturnEmptyPageForStudent() {
            Page<StudentEnrollment> emptyPage = new PageImpl<>(List.of());
            when(enrollmentRepository.findByStudentId(TENANT_ID, STUDENT_ID, 0, 10, null))
                    .thenReturn(emptyPage);

            Page<EnrollmentSummary> result = service.byStudent(TENANT_ID, STUDENT_ID, 0, 10, null);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }
}
