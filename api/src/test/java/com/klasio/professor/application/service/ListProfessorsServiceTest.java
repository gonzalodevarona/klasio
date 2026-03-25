package com.klasio.professor.application.service;

import com.klasio.professor.application.dto.ProfessorSummary;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.model.ProfessorId;
import com.klasio.professor.domain.model.ProfessorStatus;
import com.klasio.professor.domain.port.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListProfessorsServiceTest {

    @Mock
    private ProfessorRepository professorRepository;

    private ListProfessorsService service;

    private static final UUID TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ListProfessorsService(professorRepository);
    }

    @Test
    @DisplayName("should return paginated results when no status filter is applied")
    void execute_withNoStatusFilter_returnsAllProfessors() {
        Pageable pageable = PageRequest.of(0, 20);
        Professor professor = Professor.reconstitute(
                ProfessorId.of(UUID.randomUUID()), TENANT_ID,
                "Carlos", "Martinez", "carlos@example.com", null,
                ProfessorStatus.ACTIVE, null, null,
                Instant.now(), UUID.randomUUID(), null, null
        );
        Page<Professor> page = new PageImpl<>(List.of(professor), pageable, 1);

        when(professorRepository.findAllByTenant(TENANT_ID, pageable, null)).thenReturn(page);

        Page<ProfessorSummary> result = service.execute(TENANT_ID, pageable, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).firstName()).isEqualTo("Carlos");
        assertThat(result.getContent().get(0).lastName()).isEqualTo("Martinez");
        verify(professorRepository).findAllByTenant(TENANT_ID, pageable, null);
    }

    @Test
    @DisplayName("should pass parsed status filter to repository")
    void execute_withStatusFilter_filtersCorrectly() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Professor> page = new PageImpl<>(List.of(), pageable, 0);

        when(professorRepository.findAllByTenant(TENANT_ID, pageable, ProfessorStatus.ACTIVE)).thenReturn(page);

        service.execute(TENANT_ID, pageable, "ACTIVE");

        verify(professorRepository).findAllByTenant(TENANT_ID, pageable, ProfessorStatus.ACTIVE);
    }

    @Test
    @DisplayName("should return correct page metadata")
    void execute_returnsPageMetadata() {
        Pageable pageable = PageRequest.of(0, 10);
        Professor professor1 = Professor.reconstitute(
                ProfessorId.of(UUID.randomUUID()), TENANT_ID,
                "Carlos", "Martinez", "carlos@example.com", null,
                ProfessorStatus.ACTIVE, null, null,
                Instant.now(), UUID.randomUUID(), null, null
        );
        Professor professor2 = Professor.reconstitute(
                ProfessorId.of(UUID.randomUUID()), TENANT_ID,
                "Ana", "Lopez", "ana@example.com", null,
                ProfessorStatus.ACTIVE, null, null,
                Instant.now(), UUID.randomUUID(), null, null
        );
        Page<Professor> page = new PageImpl<>(List.of(professor1, professor2), pageable, 25);

        when(professorRepository.findAllByTenant(TENANT_ID, pageable, null)).thenReturn(page);

        Page<ProfessorSummary> result = service.execute(TENANT_ID, pageable, null);

        assertThat(result.getTotalElements()).isEqualTo(25);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
    }
}
