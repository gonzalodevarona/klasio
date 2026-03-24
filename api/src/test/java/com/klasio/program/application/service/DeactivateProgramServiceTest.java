package com.klasio.program.application.service;

import com.klasio.program.domain.event.ProgramDeactivated;
import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.model.ProgramStatus;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.infrastructure.exception.ProgramAlreadyInactiveException;
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
class DeactivateProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeactivateProgramService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID DEACTIVATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DeactivateProgramService(programRepository, eventPublisher);
    }

    @Test
    @DisplayName("should deactivate program, save it, and publish domain events")
    void execute_deactivatesAndSaves() {
        Program program = Program.create(TENANT_ID, "Kids Program", UUID.randomUUID());
        UUID programId = program.getId().value();

        when(programRepository.findById(TENANT_ID, programId)).thenReturn(Optional.of(program));

        Program result = service.execute(TENANT_ID, programId, DEACTIVATED_BY);

        assertThat(result.getStatus()).isEqualTo(ProgramStatus.INACTIVE);

        verify(programRepository).save(program);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .anySatisfy(event -> assertThat(event).isInstanceOf(ProgramDeactivated.class));
    }

    @Test
    @DisplayName("should throw ProgramNotFoundException when program does not exist")
    void execute_whenNotFound_throwsProgramNotFoundException() {
        UUID programId = UUID.randomUUID();
        when(programRepository.findById(TENANT_ID, programId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, programId, DEACTIVATED_BY))
                .isInstanceOf(ProgramNotFoundException.class);

        verify(programRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw ProgramAlreadyInactiveException when program is already inactive")
    void execute_whenAlreadyInactive_throwsProgramAlreadyInactiveException() {
        Program program = Program.create(TENANT_ID, "Kids Program", UUID.randomUUID());
        program.deactivate(UUID.randomUUID());
        UUID programId = program.getId().value();

        when(programRepository.findById(TENANT_ID, programId)).thenReturn(Optional.of(program));

        assertThatThrownBy(() -> service.execute(TENANT_ID, programId, DEACTIVATED_BY))
                .isInstanceOf(ProgramAlreadyInactiveException.class);

        verify(programRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
