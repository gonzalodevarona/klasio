package com.klasio.program.application.service;

import com.klasio.program.application.dto.CreateProgramCommand;
import com.klasio.program.domain.event.ProgramCreated;
import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.model.ProgramStatus;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.infrastructure.exception.ProgramNameAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateProgramService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CreateProgramService(programRepository, eventPublisher);
    }

    @Test
    @DisplayName("should create program, save it, and publish domain events")
    void execute_withValidCommand_createsAndSavesProgram() {
        when(programRepository.existsByNameInTenant(TENANT_ID, "Kids Program")).thenReturn(false);

        CreateProgramCommand command = new CreateProgramCommand(
                TENANT_ID,
                "Kids Program",
                CREATED_BY
        );

        Program result = service.execute(command);

        assertThat(result).isNotNull();

        ArgumentCaptor<Program> programCaptor = ArgumentCaptor.forClass(Program.class);
        verify(programRepository).save(programCaptor.capture());
        assertThat(programCaptor.getValue().getName()).isEqualTo("Kids Program");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ProgramCreated.class);

        ProgramCreated event = (ProgramCreated) eventCaptor.getValue();
        assertThat(event.name()).isEqualTo("Kids Program");
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.createdBy()).isEqualTo(CREATED_BY);
    }

    @Test
    @DisplayName("should throw ProgramNameAlreadyExistsException when name is duplicate in tenant")
    void execute_withDuplicateName_throwsProgramNameAlreadyExistsException() {
        when(programRepository.existsByNameInTenant(TENANT_ID, "Kids Program")).thenReturn(true);

        CreateProgramCommand command = new CreateProgramCommand(
                TENANT_ID,
                "Kids Program",
                CREATED_BY
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ProgramNameAlreadyExistsException.class);

        verify(programRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should return created program with correct fields")
    void execute_returnsCreatedProgram() {
        when(programRepository.existsByNameInTenant(TENANT_ID, "Youth & Adults")).thenReturn(false);

        CreateProgramCommand command = new CreateProgramCommand(
                TENANT_ID,
                "Youth & Adults",
                CREATED_BY
        );

        Program result = service.execute(command);

        assertThat(result.getName()).isEqualTo("Youth & Adults");
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getStatus()).isEqualTo(ProgramStatus.ACTIVE);
        assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getId()).isNotNull();
    }
}
