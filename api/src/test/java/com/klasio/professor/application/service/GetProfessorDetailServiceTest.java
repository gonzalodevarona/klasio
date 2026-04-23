package com.klasio.professor.application.service;

import com.klasio.professor.application.dto.ProfessorDetail;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.model.ProfessorId;
import com.klasio.professor.domain.model.ProfessorStatus;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.shared.domain.port.UserDisplayNamePort;
import com.klasio.shared.infrastructure.exception.ProfessorNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProfessorDetailServiceTest {

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private UserDisplayNamePort userDisplayNamePort;

    private GetProfessorDetailService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GetProfessorDetailService(professorRepository, userDisplayNamePort);
    }

    @Test
    @DisplayName("should resolve createdBy UUID to display name")
    void execute_resolvesCreatedByToDisplayName() {
        UUID professorId = UUID.randomUUID();
        Professor professor = Professor.reconstitute(
                ProfessorId.of(professorId), TENANT_ID, null,
                "Carlos", "Martinez", "carlos@example.com", null,
                ProfessorStatus.ACTIVE, null, null,
                Instant.now(), CREATED_BY, null, null,
                IdentityDocumentType.CC, "12345678"
        );

        when(professorRepository.findById(TENANT_ID, professorId)).thenReturn(Optional.of(professor));
        when(userDisplayNamePort.findDisplayName(CREATED_BY)).thenReturn(Optional.of("Maria Lopez"));

        ProfessorDetail result = service.execute(TENANT_ID, professorId);

        assertThat(result.createdBy()).isEqualTo("Maria Lopez");
    }

    @Test
    @DisplayName("should fall back to UUID string when user not found")
    void execute_fallsBackToUuidStringWhenUserNotFound() {
        UUID professorId = UUID.randomUUID();
        Professor professor = Professor.reconstitute(
                ProfessorId.of(professorId), TENANT_ID, null,
                "Carlos", "Martinez", "carlos@example.com", null,
                ProfessorStatus.ACTIVE, null, null,
                Instant.now(), CREATED_BY, null, null,
                IdentityDocumentType.CC, "12345678"
        );

        when(professorRepository.findById(TENANT_ID, professorId)).thenReturn(Optional.of(professor));
        when(userDisplayNamePort.findDisplayName(CREATED_BY)).thenReturn(Optional.empty());

        ProfessorDetail result = service.execute(TENANT_ID, professorId);

        assertThat(result.createdBy()).isEqualTo(CREATED_BY.toString());
    }

    @Test
    @DisplayName("should return ProfessorDetail when professor exists")
    void execute_withExistingId_returnsProfessorDetail() {
        UUID professorId = UUID.randomUUID();
        Professor professor = Professor.reconstitute(
                ProfessorId.of(professorId), TENANT_ID, null,
                "Carlos", "Martinez", "carlos@example.com", null,
                ProfessorStatus.ACTIVE, null, null,
                Instant.now(), UUID.randomUUID(), null, null,
                IdentityDocumentType.CC, "12345678"
        );

        when(professorRepository.findById(TENANT_ID, professorId)).thenReturn(Optional.of(professor));
        when(userDisplayNamePort.findDisplayName(any())).thenReturn(Optional.empty());

        ProfessorDetail result = service.execute(TENANT_ID, professorId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(professorId);
        assertThat(result.firstName()).isEqualTo("Carlos");
        assertThat(result.lastName()).isEqualTo("Martinez");
        assertThat(result.email()).isEqualTo("carlos@example.com");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("should throw ProfessorNotFoundException when professor does not exist")
    void execute_withNonExistingId_throwsNotFoundException() {
        when(professorRepository.findById(TENANT_ID, PROFESSOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, PROFESSOR_ID))
                .isInstanceOf(ProfessorNotFoundException.class)
                .hasMessage("Professor not found");
    }
}
