package com.klasio.professor.application.service;

import com.klasio.professor.application.dto.CreateProfessorCommand;
import com.klasio.professor.domain.event.ProfessorCreated;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.model.ProfessorStatus;
import com.klasio.professor.domain.port.AccountSetupCreationPort;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.infrastructure.exception.ProfessorEmailAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.ProfessorIdentityNumberAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.klasio.shared.domain.model.IdentityDocumentType;

@ExtendWith(MockitoExtension.class)
class CreateProfessorServiceTest {

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AccountSetupCreationPort accountSetupCreationPort;

    private CreateProfessorService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CreateProfessorService(professorRepository, eventPublisher, accountSetupCreationPort);
    }

    @Test
    @DisplayName("should create professor, save it, publish domain events, and trigger account setup")
    void execute_withValidCommand_createsProfessorAndDispatchesSetup() {
        UUID userId = UUID.randomUUID();
        when(professorRepository.existsByEmailInTenant(TENANT_ID, "carlos@example.com")).thenReturn(false);
        when(accountSetupCreationPort.createAndDispatchSetup(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(userId);

        CreateProfessorCommand command = new CreateProfessorCommand(
                TENANT_ID,
                "Carlos",
                "Martinez",
                "carlos@example.com",
                "+573001234567", IdentityDocumentType.CC, "12345678", CREATED_BY);

        Professor result = service.execute(command);

        assertThat(result).isNotNull();

        ArgumentCaptor<Professor> professorCaptor = ArgumentCaptor.forClass(Professor.class);
        verify(professorRepository).save(professorCaptor.capture());
        assertThat(professorCaptor.getValue().getFirstName()).isEqualTo("Carlos");
        assertThat(professorCaptor.getValue().getLastName()).isEqualTo("Martinez");
        assertThat(professorCaptor.getValue().getEmail()).isEqualTo("carlos@example.com");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ProfessorCreated.class);

        ProfessorCreated event = (ProfessorCreated) eventCaptor.getValue();
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.createdBy()).isEqualTo(CREATED_BY);
        assertThat(event.invitationExpiresAt()).isNotNull();
        assertThat(event.invitationExpiresAt()).isAfter(Instant.now().minus(1, ChronoUnit.SECONDS));

        // Verify account setup dispatched for the new professor
        verify(accountSetupCreationPort).createAndDispatchSetup(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("should throw ProfessorEmailAlreadyExistsException when email is duplicate in tenant")
    void execute_withDuplicateEmail_throwsException() {
        when(professorRepository.existsByEmailInTenant(TENANT_ID, "carlos@example.com")).thenReturn(true);

        CreateProfessorCommand command = new CreateProfessorCommand(
                TENANT_ID,
                "Carlos",
                "Martinez",
                "carlos@example.com",
                "+573001234567", IdentityDocumentType.CC, "12345678", CREATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ProfessorEmailAlreadyExistsException.class);

        verify(professorRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(accountSetupCreationPort, never()).createAndDispatchSetup(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("should throw ProfessorIdentityNumberAlreadyExistsException when identity number is duplicate in tenant")
    void execute_withDuplicateIdentityNumber_throwsException() {
        when(professorRepository.existsByEmailInTenant(TENANT_ID, "carlos@example.com")).thenReturn(false);
        when(professorRepository.existsByIdentityNumberInTenant(TENANT_ID, "12345678")).thenReturn(true);

        CreateProfessorCommand command = new CreateProfessorCommand(
                TENANT_ID, "Carlos", "Martinez", "carlos@example.com", "+573001234567",
                IdentityDocumentType.CC, "12345678", CREATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ProfessorIdentityNumberAlreadyExistsException.class);

        verify(professorRepository, never()).save(any());
        verify(accountSetupCreationPort, never()).createAndDispatchSetup(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("should return created professor with correct fields")
    void execute_returnsCreatedProfessor() {
        UUID userId = UUID.randomUUID();
        when(professorRepository.existsByEmailInTenant(TENANT_ID, "carlos@example.com")).thenReturn(false);
        when(accountSetupCreationPort.createAndDispatchSetup(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(userId);

        CreateProfessorCommand command = new CreateProfessorCommand(
                TENANT_ID,
                "Carlos",
                "Martinez",
                "carlos@example.com",
                "+573001234567", IdentityDocumentType.CC, "12345678", CREATED_BY);

        Professor result = service.execute(command);

        assertThat(result.getFirstName()).isEqualTo("Carlos");
        assertThat(result.getLastName()).isEqualTo("Martinez");
        assertThat(result.getEmail()).isEqualTo("carlos@example.com");
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getStatus()).isEqualTo(ProfessorStatus.INVITED);
        assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getId()).isNotNull();
    }
}
