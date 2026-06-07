package com.klasio.student.application.service;

import com.klasio.shared.infrastructure.exception.StudentEmailAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.StudentIdentityNumberAlreadyExistsException;
import com.klasio.student.application.dto.CreateStudentCommand;
import com.klasio.student.domain.event.StudentCreated;
import com.klasio.student.domain.model.BloodType;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.AccountSetupCreationPort;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateStudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AccountSetupCreationPort accountSetupCreationPort;

    private CreateStudentService service;

    private static final LocalDate ADULT_DATE_OF_BIRTH = LocalDate.of(2000, 1, 15);

    @BeforeEach
    void setUp() {
        service = new CreateStudentService(studentRepository, eventPublisher, accountSetupCreationPort);
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

    private CreateStudentCommand sampleCommand(UUID tenantId, String email, String identityNumber) {
        return new CreateStudentCommand(
                tenantId, "Ana", "Lopez", email,
                LocalDate.of(1995, 1, 1), "Sanitas", identityNumber, IdentityDocumentType.CC,
                BloodType.O_POSITIVE, "3000000000",
                null, null, null, null, null,
                UUID.randomUUID()
        );
    }

    @Test
    @DisplayName("should create student, save it, publish domain events, and trigger account setup")
    void happyPath_createsStudentAndPublishesEventsAndDispatchesSetup() {
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(studentRepository.existsByEmailInTenant(eq(tenantId), anyString())).thenReturn(false);
        when(accountSetupCreationPort.createAndDispatchSetup(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(userId);

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
        verify(studentRepository, times(2)).save(studentCaptor.capture());
        assertThat(studentCaptor.getAllValues().get(0).getFirstName()).isEqualTo("Carlos");
        // Second save links userId after account creation
        assertThat(studentCaptor.getAllValues().get(1).getUserId()).isEqualTo(userId);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(StudentCreated.class);

        StudentCreated event = (StudentCreated) eventCaptor.getValue();
        assertThat(event.firstName()).isEqualTo("Carlos");
        assertThat(event.lastName()).isEqualTo("Garcia");
        assertThat(event.email()).isEqualTo("carlos.garcia@example.com");
        assertThat(event.createdBy()).isEqualTo(createdBy);

        // Verify account setup was triggered for the newly created student
        verify(accountSetupCreationPort).createAndDispatchSetup(
                eq(tenantId),
                eq("carlos.garcia@example.com"),
                any(), any(),
                eq(IdentityDocumentType.CC),
                eq("1234567890"),
                eq("3001234567")
        );
    }

    @Test
    @DisplayName("should throw StudentIdentityNumberAlreadyExistsException when identity number is duplicate in tenant")
    void execute_rejectsDuplicateIdentityNumberInTenant() {
        UUID tenantId = UUID.randomUUID();
        when(studentRepository.existsByEmailInTenant(eq(tenantId), anyString())).thenReturn(false);
        when(studentRepository.existsByIdentityNumberInTenant(eq(tenantId), eq("CC-100"))).thenReturn(true);

        CreateStudentCommand cmd = sampleCommand(tenantId, "new@x.com", "CC-100");

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(StudentIdentityNumberAlreadyExistsException.class);
        verify(accountSetupCreationPort, never()).createAndDispatchSetup(any(), any(), any(), any(), any(), any(), any());
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
        verify(accountSetupCreationPort, never()).createAndDispatchSetup(
                any(), any(), any(), any(), any(), any(), any());
    }
}
