package com.klasio.programclass.application.service;

import com.klasio.attendance.application.port.input.CancelMismatchingFutureRegistrationsUseCase;
import com.klasio.programclass.application.dto.UpdateClassCommand;
import com.klasio.programclass.domain.event.ClassUpdated;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassScheduleEntry;
import com.klasio.programclass.domain.model.ClassType;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import com.klasio.shared.infrastructure.exception.ClassNameAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateClassServiceTest {

    @Mock
    private ProgramClassRepository programClassRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CancelMismatchingFutureRegistrationsUseCase cancelMismatchingFutureRegistrationsUseCase;

    private UpdateClassService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();
    private static final UUID UPDATED_BY = UUID.randomUUID();

    private static final ClassScheduleEntry MONDAY_SCHEDULE = new ClassScheduleEntry(
            DayOfWeek.MONDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0));

    private static final ClassScheduleEntry WEDNESDAY_SCHEDULE = new ClassScheduleEntry(
            DayOfWeek.WEDNESDAY, null, LocalTime.of(16, 0), LocalTime.of(18, 0));

    @BeforeEach
    void setUp() {
        service = new UpdateClassService(programClassRepository, eventPublisher, cancelMismatchingFutureRegistrationsUseCase);
    }

    private ProgramClass createExistingClass(String name, ClassLevel level, int maxStudents) {
        ProgramClass pc = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, name, level, ClassType.RECURRING,
                List.of(MONDAY_SCHEDULE), null, maxStudents, CREATED_BY);
        pc.clearDomainEvents();
        return pc;
    }

    // ---- T090: Happy path - update class ----

    @Test
    @DisplayName("should update name, level, schedule, and maxStudents, then publish ClassUpdated event")
    void execute_withValidCommand_updatesClassAndPublishesEvent() {
        ProgramClass existing = createExistingClass("Kids Beginner Monday", ClassLevel.BEGINNER, 20);
        UUID classId = existing.getId().value();

        when(programClassRepository.findById(TENANT_ID, classId)).thenReturn(Optional.of(existing));
        when(programClassRepository.existsByNameInProgramExcluding(PROGRAM_ID, "Kids Intermediate MoWe", classId))
                .thenReturn(false);

        UpdateClassCommand command = new UpdateClassCommand(
                TENANT_ID, PROGRAM_ID, classId, "Kids Intermediate MoWe",
                ClassLevel.INTERMEDIATE, List.of(MONDAY_SCHEDULE, WEDNESDAY_SCHEDULE), 25, UPDATED_BY);

        ProgramClass result = service.execute(command);

        assertThat(result.getName()).isEqualTo("Kids Intermediate MoWe");
        assertThat(result.getLevel()).isEqualTo(ClassLevel.INTERMEDIATE);
        assertThat(result.getScheduleEntries()).hasSize(2);
        assertThat(result.getMaxStudents()).isEqualTo(25);
        assertThat(result.getUpdatedBy()).isEqualTo(UPDATED_BY);
        assertThat(result.getUpdatedAt()).isNotNull();

        ArgumentCaptor<ProgramClass> classCaptor = ArgumentCaptor.forClass(ProgramClass.class);
        verify(programClassRepository).save(classCaptor.capture());
        assertThat(classCaptor.getValue().getName()).isEqualTo("Kids Intermediate MoWe");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ClassUpdated.class);

        ClassUpdated event = (ClassUpdated) eventCaptor.getValue();
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.programId()).isEqualTo(PROGRAM_ID);
        assertThat(event.name()).isEqualTo("Kids Intermediate MoWe");
        assertThat(event.level()).isEqualTo(ClassLevel.INTERMEDIATE.name());
        assertThat(event.maxStudents()).isEqualTo(25);
        assertThat(event.updatedBy()).isEqualTo(UPDATED_BY);
    }

    // ---- T091: Duplicate name on update rejected ----

    @Test
    @DisplayName("should throw ClassNameAlreadyExistsException when updated name conflicts with another class")
    void execute_withDuplicateName_throwsException() {
        ProgramClass existing = createExistingClass("Kids Beginner Monday", ClassLevel.BEGINNER, 20);
        UUID classId = existing.getId().value();

        when(programClassRepository.findById(TENANT_ID, classId)).thenReturn(Optional.of(existing));
        when(programClassRepository.existsByNameInProgramExcluding(PROGRAM_ID, "Advanced Tuesday", classId))
                .thenReturn(true);

        UpdateClassCommand command = new UpdateClassCommand(
                TENANT_ID, PROGRAM_ID, classId, "Advanced Tuesday",
                ClassLevel.ADVANCED, List.of(MONDAY_SCHEDULE), 15, UPDATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ClassNameAlreadyExistsException.class);

        verify(programClassRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ---- T092: Class not found ----

    @Test
    @DisplayName("should throw ClassNotFoundException when class does not exist")
    void execute_withNonExistentClass_throwsClassNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();

        when(programClassRepository.findById(TENANT_ID, nonExistentId)).thenReturn(Optional.empty());

        UpdateClassCommand command = new UpdateClassCommand(
                TENANT_ID, PROGRAM_ID, nonExistentId, "Any Name",
                ClassLevel.BEGINNER, List.of(MONDAY_SCHEDULE), 20, UPDATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining(nonExistentId.toString());

        verify(programClassRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ---- T093: Validation failure - blank name ----

    @Test
    @DisplayName("should throw IllegalArgumentException when name is blank")
    void execute_withBlankName_throwsIllegalArgumentException() {
        ProgramClass existing = createExistingClass("Kids Beginner Monday", ClassLevel.BEGINNER, 20);
        UUID classId = existing.getId().value();

        when(programClassRepository.findById(TENANT_ID, classId)).thenReturn(Optional.of(existing));
        when(programClassRepository.existsByNameInProgramExcluding(PROGRAM_ID, "   ", classId))
                .thenReturn(false);

        UpdateClassCommand command = new UpdateClassCommand(
                TENANT_ID, PROGRAM_ID, classId, "   ",
                ClassLevel.BEGINNER, List.of(MONDAY_SCHEDULE), 20, UPDATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalArgumentException.class);

        verify(programClassRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
