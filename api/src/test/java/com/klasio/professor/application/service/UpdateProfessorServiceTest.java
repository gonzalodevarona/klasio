package com.klasio.professor.application.service;

import com.klasio.professor.application.dto.UpdateProfessorCommand;
import com.klasio.professor.domain.event.ProfessorUpdated;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.model.ProfessorId;
import com.klasio.professor.domain.model.ProfessorStatus;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.infrastructure.exception.ProfessorEmailAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.ProfessorNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProfessorServiceTest {

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UpdateProfessorService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID UPDATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new UpdateProfessorService(professorRepository, eventPublisher);
    }

    @Test
    @DisplayName("should update professor, save it, and publish domain events")
    void execute_withValidCommand_updatesProfessor() {
        UUID professorId = UUID.randomUUID();
        Professor professor = Professor.reconstitute(
                ProfessorId.of(professorId), TENANT_ID,
                "Carlos", "Martinez", "carlos@example.com", null,
                ProfessorStatus.ACTIVE, null, null,
                Instant.now(), UUID.randomUUID(), null, null
        );

        when(professorRepository.findById(TENANT_ID, professorId)).thenReturn(Optional.of(professor));
        when(professorRepository.existsByEmailInTenantExcluding(TENANT_ID, "ana@example.com", professorId)).thenReturn(false);

        UpdateProfessorCommand command = new UpdateProfessorCommand(
                TENANT_ID, professorId, "Ana", "Lopez", "ana@example.com", "+573009876543", UPDATED_BY);

        Professor result = service.execute(command);

        assertThat(result.getFirstName()).isEqualTo("Ana");
        assertThat(result.getLastName()).isEqualTo("Lopez");
        assertThat(result.getEmail()).isEqualTo("ana@example.com");

        verify(professorRepository).save(professor);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .anySatisfy(event -> assertThat(event).isInstanceOf(ProfessorUpdated.class));
    }

    @Test
    @DisplayName("should throw ProfessorNotFoundException when professor does not exist")
    void execute_withNonExistingId_throwsNotFoundException() {
        UUID professorId = UUID.randomUUID();
        when(professorRepository.findById(TENANT_ID, professorId)).thenReturn(Optional.empty());

        UpdateProfessorCommand command = new UpdateProfessorCommand(
                TENANT_ID, professorId, "Ana", "Lopez", "ana@example.com", "+573009876543", UPDATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ProfessorNotFoundException.class);

        verify(professorRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw ProfessorEmailAlreadyExistsException when email is duplicate")
    void execute_withDuplicateEmail_throwsException() {
        UUID professorId = UUID.randomUUID();
        Professor professor = Professor.reconstitute(
                ProfessorId.of(professorId), TENANT_ID,
                "Carlos", "Martinez", "carlos@example.com", null,
                ProfessorStatus.ACTIVE, null, null,
                Instant.now(), UUID.randomUUID(), null, null
        );

        when(professorRepository.findById(TENANT_ID, professorId)).thenReturn(Optional.of(professor));
        when(professorRepository.existsByEmailInTenantExcluding(TENANT_ID, "ana@example.com", professorId)).thenReturn(true);

        UpdateProfessorCommand command = new UpdateProfessorCommand(
                TENANT_ID, professorId, "Ana", "Lopez", "ana@example.com", "+573009876543", UPDATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ProfessorEmailAlreadyExistsException.class);

        verify(professorRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
