package com.klasio.student.application.service;

import com.klasio.shared.infrastructure.exception.StudentEmailAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.StudentNotFoundException;
import com.klasio.student.application.dto.UpdateStudentCommand;
import com.klasio.student.domain.event.StudentUpdated;
import com.klasio.student.domain.model.BloodType;
import com.klasio.student.domain.model.IdentityDocumentType;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.model.StudentId;
import com.klasio.student.domain.port.StudentRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateStudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UpdateStudentService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_BY = UUID.randomUUID();
    private static final LocalDate ADULT_DATE_OF_BIRTH = LocalDate.of(2000, 1, 15);

    @BeforeEach
    void setUp() {
        service = new UpdateStudentService(studentRepository, eventPublisher);
    }

    private Student createExistingStudent() {
        return Student.reconstitute(
                StudentId.of(STUDENT_ID), TENANT_ID, "Carlos", "Garcia",
                "carlos@example.com",
                ADULT_DATE_OF_BIRTH, "Sura", "1234567890", IdentityDocumentType.CC,
                BloodType.O_POSITIVE, "3001234567",
                null, null, null, null, null,
                "ACTIVE", Instant.now(), UUID.randomUUID(),
                null, null, null, null
        );
    }

    private UpdateStudentCommand buildUpdateCommand(String email) {
        return new UpdateStudentCommand(
                TENANT_ID, STUDENT_ID, "Maria", "Lopez", email,
                LocalDate.of(1995, 6, 20), "Nueva EPS", "9876543210", IdentityDocumentType.CC,
                BloodType.A_POSITIVE, "3005555555",
                null, null, null, null, null,
                UPDATED_BY
        );
    }

    @Test
    @DisplayName("should find student, update it, save it, and publish events")
    void happyPath_updatesStudentAndPublishesEvents() {
        Student existingStudent = createExistingStudent();
        when(studentRepository.findById(TENANT_ID, STUDENT_ID))
                .thenReturn(Optional.of(existingStudent));
        when(studentRepository.existsByEmailInTenantExcluding(TENANT_ID, "maria.lopez@example.com", STUDENT_ID))
                .thenReturn(false);

        UpdateStudentCommand command = buildUpdateCommand("maria.lopez@example.com");

        Student result = service.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Maria");
        assertThat(result.getLastName()).isEqualTo("Lopez");
        assertThat(result.getEmail()).isEqualTo("maria.lopez@example.com");
        assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 6, 20));
        assertThat(result.getEps()).isEqualTo("Nueva EPS");
        assertThat(result.getIdentityNumber()).isEqualTo("9876543210");
        assertThat(result.getIdentityDocumentType()).isEqualTo(IdentityDocumentType.CC);
        assertThat(result.getBloodType()).isEqualTo(BloodType.A_POSITIVE);

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        assertThat(studentCaptor.getValue().getFirstName()).isEqualTo("Maria");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(StudentUpdated.class);

        StudentUpdated event = (StudentUpdated) eventCaptor.getValue();
        assertThat(event.firstName()).isEqualTo("Maria");
        assertThat(event.lastName()).isEqualTo("Lopez");
        assertThat(event.email()).isEqualTo("maria.lopez@example.com");
        assertThat(event.updatedBy()).isEqualTo(UPDATED_BY);
    }

    @Test
    @DisplayName("should throw StudentNotFoundException when student does not exist")
    void studentNotFound_throwsStudentNotFoundException() {
        when(studentRepository.findById(TENANT_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        UpdateStudentCommand command = buildUpdateCommand("maria.lopez@example.com");

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining(STUDENT_ID.toString());

        verify(studentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw StudentEmailAlreadyExistsException when email is taken by another student")
    void duplicateEmail_throwsStudentEmailAlreadyExistsException() {
        Student existingStudent = createExistingStudent();
        when(studentRepository.findById(TENANT_ID, STUDENT_ID))
                .thenReturn(Optional.of(existingStudent));
        when(studentRepository.existsByEmailInTenantExcluding(
                eq(TENANT_ID), eq("taken@example.com"), eq(STUDENT_ID)))
                .thenReturn(true);

        UpdateStudentCommand command = new UpdateStudentCommand(
                TENANT_ID, STUDENT_ID, "Maria", "Lopez", "taken@example.com",
                ADULT_DATE_OF_BIRTH, "Sura", "1234567890", IdentityDocumentType.CC,
                BloodType.O_POSITIVE, "3001234567",
                null, null, null, null, null,
                UPDATED_BY
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(StudentEmailAlreadyExistsException.class)
                .hasMessageContaining("taken@example.com");

        verify(studentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
