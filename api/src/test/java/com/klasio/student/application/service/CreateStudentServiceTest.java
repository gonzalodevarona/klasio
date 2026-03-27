package com.klasio.student.application.service;

import com.klasio.shared.infrastructure.exception.StudentEmailAlreadyExistsException;
import com.klasio.student.application.dto.CreateStudentCommand;
import com.klasio.student.domain.event.StudentCreated;
import com.klasio.student.domain.model.BloodType;
import com.klasio.student.domain.model.IdentityDocumentType;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateStudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateStudentService service;

    private static final LocalDate ADULT_DATE_OF_BIRTH = LocalDate.of(2000, 1, 15);

    @BeforeEach
    void setUp() {
        service = new CreateStudentService(studentRepository, eventPublisher);
    }

    private CreateStudentCommand buildAdultCommand(UUID tenantId, UUID createdBy) {
        return new CreateStudentCommand(
                tenantId, "Carlos", "Garcia", "carlos.garcia@example.com",
                ADULT_DATE_OF_BIRTH, "Sura", "1234567890", IdentityDocumentType.CC,
                BloodType.O_POSITIVE, "3001234567",
                null, null, null, null, null,
                createdBy
        );
    }

    @Test
    @DisplayName("should create student, save it, and publish domain events")
    void happyPath_createsStudentAndPublishesEvents() {
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        when(studentRepository.existsByEmailInTenant(eq(tenantId), anyString())).thenReturn(false);

        CreateStudentCommand command = buildAdultCommand(tenantId, createdBy);

        Student result = service.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Carlos");
        assertThat(result.getLastName()).isEqualTo("Garcia");
        assertThat(result.getEmail()).isEqualTo("carlos.garcia@example.com");
        assertThat(result.getDateOfBirth()).isEqualTo(ADULT_DATE_OF_BIRTH);
        assertThat(result.getEps()).isEqualTo("Sura");
        assertThat(result.getIdentityNumber()).isEqualTo("1234567890");
        assertThat(result.getIdentityDocumentType()).isEqualTo(IdentityDocumentType.CC);
        assertThat(result.getBloodType()).isEqualTo(BloodType.O_POSITIVE);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        assertThat(studentCaptor.getValue().getFirstName()).isEqualTo("Carlos");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(StudentCreated.class);

        StudentCreated event = (StudentCreated) eventCaptor.getValue();
        assertThat(event.firstName()).isEqualTo("Carlos");
        assertThat(event.lastName()).isEqualTo("Garcia");
        assertThat(event.email()).isEqualTo("carlos.garcia@example.com");
        assertThat(event.createdBy()).isEqualTo(createdBy);
    }

    @Test
    @DisplayName("should throw StudentEmailAlreadyExistsException when email is duplicate in tenant")
    void duplicateEmail_throwsStudentEmailAlreadyExistsException() {
        UUID tenantId = UUID.randomUUID();
        when(studentRepository.existsByEmailInTenant(eq(tenantId), eq("carlos.garcia@example.com")))
                .thenReturn(true);

        CreateStudentCommand command = buildAdultCommand(tenantId, UUID.randomUUID());

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(StudentEmailAlreadyExistsException.class)
                .hasMessageContaining("carlos.garcia@example.com");

        verify(studentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
