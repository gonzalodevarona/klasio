package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramSummary;
import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.model.ProgramStatus;
import com.klasio.program.domain.port.ProgramRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListProgramsServiceTest {

    @Mock
    private ProgramRepository programRepository;

    private ListProgramsService service;

    private static final UUID TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ListProgramsService(programRepository);
    }

    @Test
    @DisplayName("should return paginated results mapped to ProgramSummary")
    void execute_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Program program = Program.create(TENANT_ID, "Kids Program", null, UUID.randomUUID());
        Page<Program> page = new PageImpl<>(List.of(program), pageable, 1);

        when(programRepository.findAllByTenant(TENANT_ID, pageable, null)).thenReturn(page);

        Page<ProgramSummary> result = service.execute(TENANT_ID, pageable, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Kids Program");
        verify(programRepository).findAllByTenant(TENANT_ID, pageable, null);
    }

    @Test
    @DisplayName("should pass parsed status filter to repository")
    void execute_withStatusFilter_passesStatusToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Program> page = new PageImpl<>(List.of(), pageable, 0);

        when(programRepository.findAllByTenant(TENANT_ID, pageable, ProgramStatus.ACTIVE)).thenReturn(page);

        service.execute(TENANT_ID, pageable, "ACTIVE");

        verify(programRepository).findAllByTenant(TENANT_ID, pageable, ProgramStatus.ACTIVE);
    }

    @Test
    @DisplayName("should pass null status to repository when status parameter is null")
    void execute_withNullStatus_passesNullToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Program> page = new PageImpl<>(List.of(), pageable, 0);

        when(programRepository.findAllByTenant(TENANT_ID, pageable, null)).thenReturn(page);

        service.execute(TENANT_ID, pageable, null);

        verify(programRepository).findAllByTenant(TENANT_ID, pageable, null);
    }
}
