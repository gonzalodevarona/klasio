package com.klasio.professor.application.service;

import com.klasio.professor.domain.event.ProfessorDeactivated;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.model.ProfessorId;
import com.klasio.professor.domain.model.ProfessorStatus;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.infrastructure.exception.ProfessorAlreadyInactiveException;
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
import com.klasio.shared.domain.model.IdentityDocumentType;

@ExtendWith(MockitoExtension.class)
class DeactivateProfessorServiceTest {

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeactivateProfessorService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID DEACTIVATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DeactivateProfessorService(professorRepository, eventPublisher);
    }

    @Test
    @DisplayName("should deactivate professor, save it, and publish domain events")
    void execute_withActiveProfessor_deactivates() {
        UUID professorId = UUID.randomUUID();
        Professor professor = Professor.reconstitute(
                ProfessorId.of(professorId), TENANT_ID,
                "Carlos", "Martinez", "carlos@example.com", null,
                ProfessorStatus.ACTIVE, null, null,
                Instant.now(), UUID.randomUUID(), null, null,
                com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678"
        );

        when(professorRepository.findById(TENANT_ID, professorId)).thenReturn(Optional.of(professor));

        Professor result = service.execute(TENANT_ID, professorId, DEACTIVATED_BY);

        assertThat(result.getStatus()).isEqualTo(ProfessorStatus.DEACTIVATED);

        verify(professorRepository).save(professor);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .anySatisfy(event -> assertThat(event).isInstanceOf(ProfessorDeactivated.class));
    }

    @Test
    @DisplayName("should throw ProfessorNotFoundException when professor does not exist")
    void execute_withNonExistingId_throwsNotFoundException() {
        UUID professorId = UUID.randomUUID();
        when(professorRepository.findById(TENANT_ID, professorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, professorId, DEACTIVATED_BY))
                .isInstanceOf(ProfessorNotFoundException.class);

        verify(professorRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw ProfessorAlreadyInactiveException when professor is already deactivated")
    void execute_withAlreadyDeactivated_throwsException() {
        UUID professorId = UUID.randomUUID();
        Professor professor = Professor.reconstitute(
                ProfessorId.of(professorId), TENANT_ID,
                "Carlos", "Martinez", "carlos@example.com", null,
                ProfessorStatus.DEACTIVATED, null, null,
                Instant.now(), UUID.randomUUID(), null, null,
                com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678"
        );

        when(professorRepository.findById(TENANT_ID, professorId)).thenReturn(Optional.of(professor));

        assertThatThrownBy(() -> service.execute(TENANT_ID, professorId, DEACTIVATED_BY))
                .isInstanceOf(ProfessorAlreadyInactiveException.class);

        verify(professorRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
