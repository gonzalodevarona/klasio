package com.klasio.programclass.application.service;

import com.klasio.attendance.application.dto.CancelMismatchingFutureRegistrationsCommand;
import com.klasio.attendance.application.port.input.CancelMismatchingFutureRegistrationsUseCase;
import com.klasio.programclass.application.dto.UpdateClassCommand;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassScheduleEntry;
import com.klasio.programclass.domain.model.ClassType;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the cascade-cancel behavior in UpdateClassService:
 * when a class level changes from OPEN to a specific level (BEGINNER/INTERMEDIATE/ADVANCED),
 * future registrations that no longer match the new level must be cancelled.
 */
@ExtendWith(MockitoExtension.class)
class UpdateClassServiceLevelChangeCascadeTest {

    @Mock
    private ProgramClassRepository programClassRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CancelMismatchingFutureRegistrationsUseCase cancelMismatchingFutureRegistrationsUseCase;

    private UpdateClassService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID UPDATED_BY = UUID.randomUUID();

    private static final ClassScheduleEntry MONDAY_SCHEDULE = new ClassScheduleEntry(
            DayOfWeek.MONDAY, null, LocalTime.of(10, 0), LocalTime.of(12, 0), null);

    @BeforeEach
    void setUp() {
        service = new UpdateClassService(
                programClassRepository, eventPublisher, cancelMismatchingFutureRegistrationsUseCase);
    }

    private ProgramClass createExistingOpenClass() {
        ProgramClass pc = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, "Open Jiu-Jitsu Monday", ClassLevel.OPEN, ClassType.RECURRING,
                List.of(MONDAY_SCHEDULE), null, 20, UPDATED_BY);
        pc.clearDomainEvents();
        return pc;
    }

    private ProgramClass createExistingBeginnerClass() {
        ProgramClass pc = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, "Beginner Jiu-Jitsu Monday", ClassLevel.BEGINNER, ClassType.RECURRING,
                List.of(MONDAY_SCHEDULE), null, 20, UPDATED_BY);
        pc.clearDomainEvents();
        return pc;
    }

    // ---- T-CASCADE-01: OPEN -> BEGINNER triggers cascade ----

    @Test
    @DisplayName("should invoke cascade use case with previousLevel=OPEN and newLevel=BEGINNER when level changes from OPEN to BEGINNER")
    void execute_openToBeginner_invokesCascadeCancel() {
        ProgramClass existing = createExistingOpenClass();
        UUID classId = existing.getId().value();

        when(programClassRepository.findById(TENANT_ID, classId)).thenReturn(Optional.of(existing));
        when(programClassRepository.existsByNameInProgramExcluding(PROGRAM_ID, "Open Jiu-Jitsu Monday", classId))
                .thenReturn(false);

        UpdateClassCommand command = new UpdateClassCommand(
                TENANT_ID, PROGRAM_ID, classId, "Open Jiu-Jitsu Monday",
                ClassLevel.BEGINNER, List.of(MONDAY_SCHEDULE), 20, UPDATED_BY);

        service.execute(command);

        ArgumentCaptor<CancelMismatchingFutureRegistrationsCommand> captor =
                ArgumentCaptor.forClass(CancelMismatchingFutureRegistrationsCommand.class);
        verify(cancelMismatchingFutureRegistrationsUseCase).execute(captor.capture());

        CancelMismatchingFutureRegistrationsCommand issued = captor.getValue();
        assertThat(issued.tenantId()).isEqualTo(TENANT_ID);
        assertThat(issued.classId()).isEqualTo(classId);
        assertThat(issued.previousClassLevel()).isEqualTo("OPEN");
        assertThat(issued.newClassLevel()).isEqualTo("BEGINNER");
        assertThat(issued.actorId()).isEqualTo(UPDATED_BY);
    }

    // ---- T-CASCADE-02: BEGINNER -> OPEN does NOT trigger cascade ----

    @Test
    @DisplayName("should NOT invoke cascade use case when level changes from a specific level to OPEN")
    void execute_beginnerToOpen_doesNotInvokeCascadeCancel() {
        ProgramClass existing = createExistingBeginnerClass();
        UUID classId = existing.getId().value();

        when(programClassRepository.findById(TENANT_ID, classId)).thenReturn(Optional.of(existing));
        when(programClassRepository.existsByNameInProgramExcluding(PROGRAM_ID, "Beginner Jiu-Jitsu Monday", classId))
                .thenReturn(false);

        UpdateClassCommand command = new UpdateClassCommand(
                TENANT_ID, PROGRAM_ID, classId, "Beginner Jiu-Jitsu Monday",
                ClassLevel.OPEN, List.of(MONDAY_SCHEDULE), 20, UPDATED_BY);

        service.execute(command);

        verify(cancelMismatchingFutureRegistrationsUseCase, never()).execute(any());
    }

    // ---- T-CASCADE-03: OPEN -> OPEN does NOT trigger cascade ----

    @Test
    @DisplayName("should NOT invoke cascade use case when level stays OPEN")
    void execute_openToOpen_doesNotInvokeCascadeCancel() {
        ProgramClass existing = createExistingOpenClass();
        UUID classId = existing.getId().value();

        when(programClassRepository.findById(TENANT_ID, classId)).thenReturn(Optional.of(existing));
        when(programClassRepository.existsByNameInProgramExcluding(PROGRAM_ID, "Open Jiu-Jitsu Monday", classId))
                .thenReturn(false);

        UpdateClassCommand command = new UpdateClassCommand(
                TENANT_ID, PROGRAM_ID, classId, "Open Jiu-Jitsu Monday",
                ClassLevel.OPEN, List.of(MONDAY_SCHEDULE), 20, UPDATED_BY);

        service.execute(command);

        verify(cancelMismatchingFutureRegistrationsUseCase, never()).execute(any());
    }
}
