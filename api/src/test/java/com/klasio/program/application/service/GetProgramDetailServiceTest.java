package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramDetail;
import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.infrastructure.exception.ProgramNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProgramDetailServiceTest {

    @Mock
    private ProgramRepository programRepository;

    private GetProgramDetailService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GetProgramDetailService(programRepository);
    }

    @Test
    @DisplayName("should return ProgramDetail when program exists")
    void execute_whenProgramExists_returnsProgramDetail() {
        Program program = Program.create(TENANT_ID, "Kids Program", UUID.randomUUID());
        UUID actualProgramId = program.getId().value();

        when(programRepository.findById(TENANT_ID, actualProgramId)).thenReturn(Optional.of(program));

        ProgramDetail result = service.execute(TENANT_ID, actualProgramId);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Kids Program");
        assertThat(result.id()).isEqualTo(actualProgramId);
    }

    @Test
    @DisplayName("should throw ProgramNotFoundException when program does not exist")
    void execute_whenProgramNotFound_throwsProgramNotFoundException() {
        when(programRepository.findById(TENANT_ID, PROGRAM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, PROGRAM_ID))
                .isInstanceOf(ProgramNotFoundException.class)
                .hasMessage("Program not found");
    }
}
