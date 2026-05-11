package com.klasio.program.application.service;

import com.klasio.program.application.dto.UpdateProgramCommand;
import com.klasio.program.domain.event.ProgramUpdated;
import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.infrastructure.exception.ProgramNameAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.ProgramNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UpdateProgramService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID UPDATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new UpdateProgramService(programRepository, eventPublisher);
    }

    @Test
    @DisplayName("should update program, save it, and publish domain events")
    void execute_withValidCommand_updatesAndSaves() {
        Program program = Program.create(TENANT_ID, "Kids Program", null, UUID.randomUUID());
        UUID programId = program.getId().value();

        when(programRepository.findById(TENANT_ID, programId)).thenReturn(Optional.of(program));
        when(programRepository.existsByNameInTenantExcluding(TENANT_ID, "Updated Name", programId)).thenReturn(false);

        UpdateProgramCommand command = new UpdateProgramCommand(
                TENANT_ID, programId, "Updated Name", null, UPDATED_BY);

        Program result = service.execute(command);

        assertThat(result.getName()).isEqualTo("Updated Name");

        verify(programRepository).save(program);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .anySatisfy(event -> assertThat(event).isInstanceOf(ProgramUpdated.class));
    }

    @Test
    @DisplayName("should throw ProgramNotFoundException when program does not exist")
    void execute_whenNotFound_throwsProgramNotFoundException() {
        UUID programId = UUID.randomUUID();
        when(programRepository.findById(TENANT_ID, programId)).thenReturn(Optional.empty());

        UpdateProgramCommand command = new UpdateProgramCommand(
                TENANT_ID, programId, "Updated Name", null, UPDATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ProgramNotFoundException.class);

        verify(programRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw ProgramNameAlreadyExistsException when name is duplicate")
    void execute_whenDuplicateName_throwsProgramNameAlreadyExistsException() {
        Program program = Program.create(TENANT_ID, "Kids Program", null, UUID.randomUUID());
        UUID programId = program.getId().value();

        when(programRepository.findById(TENANT_ID, programId)).thenReturn(Optional.of(program));
        when(programRepository.existsByNameInTenantExcluding(TENANT_ID, "Duplicate Name", programId)).thenReturn(true);

        UpdateProgramCommand command = new UpdateProgramCommand(
                TENANT_ID, programId, "Duplicate Name", null, UPDATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ProgramNameAlreadyExistsException.class);

        verify(programRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
