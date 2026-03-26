package com.klasio.programclass.application.service;

import com.klasio.programclass.application.dto.CreateClassCommand;
import com.klasio.programclass.domain.event.ClassCreated;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassScheduleEntry;
import com.klasio.programclass.domain.model.ClassStatus;
import com.klasio.programclass.domain.model.ClassType;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import com.klasio.shared.infrastructure.exception.ClassNameAlreadyExistsException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateClassServiceTest {

    @Mock
    private ProgramClassRepository programClassRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateClassService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();

    private static final ClassScheduleEntry MONDAY_SCHEDULE = new ClassScheduleEntry(
            DayOfWeek.MONDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0));

    @BeforeEach
    void setUp() {
        service = new CreateClassService(programClassRepository, eventPublisher);
    }

    // ---- T034: Happy path ----

    @Test
    @DisplayName("should create recurring class, save it, and publish domain events")
    void execute_withValidRecurringCommand_createsClass() {
        when(programClassRepository.existsByNameInProgram(PROGRAM_ID, "Kids Beginner Monday")).thenReturn(false);

        CreateClassCommand command = new CreateClassCommand(
                TENANT_ID, PROGRAM_ID, "Kids Beginner Monday", ClassLevel.BEGINNER,
                ClassType.RECURRING, List.of(MONDAY_SCHEDULE), null, 20, CREATED_BY);

        ProgramClass result = service.execute(command);

        assertThat(result).isNotNull();

        ArgumentCaptor<ProgramClass> classCaptor = ArgumentCaptor.forClass(ProgramClass.class);
        verify(programClassRepository).save(classCaptor.capture());
        assertThat(classCaptor.getValue().getName()).isEqualTo("Kids Beginner Monday");
        assertThat(classCaptor.getValue().getLevel()).isEqualTo(ClassLevel.BEGINNER);
        assertThat(classCaptor.getValue().getType()).isEqualTo(ClassType.RECURRING);
        assertThat(classCaptor.getValue().getMaxStudents()).isEqualTo(20);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ClassCreated.class);

        ClassCreated event = (ClassCreated) eventCaptor.getValue();
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.programId()).isEqualTo(PROGRAM_ID);
        assertThat(event.createdBy()).isEqualTo(CREATED_BY);
    }

    @Test
    @DisplayName("should return created class with correct fields and ACTIVE status")
    void execute_returnsCreatedClassWithCorrectFields() {
        when(programClassRepository.existsByNameInProgram(PROGRAM_ID, "Advanced Wednesday")).thenReturn(false);

        CreateClassCommand command = new CreateClassCommand(
                TENANT_ID, PROGRAM_ID, "Advanced Wednesday", ClassLevel.ADVANCED,
                ClassType.RECURRING, List.of(MONDAY_SCHEDULE), null, 15, CREATED_BY);

        ProgramClass result = service.execute(command);

        assertThat(result.getName()).isEqualTo("Advanced Wednesday");
        assertThat(result.getLevel()).isEqualTo(ClassLevel.ADVANCED);
        assertThat(result.getType()).isEqualTo(ClassType.RECURRING);
        assertThat(result.getStatus()).isEqualTo(ClassStatus.ACTIVE);
        assertThat(result.getMaxStudents()).isEqualTo(15);
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getProgramId()).isEqualTo(PROGRAM_ID);
        assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getId()).isNotNull();
    }

    // ---- T035: Duplicate name rejection ----

    @Test
    @DisplayName("should throw ClassNameAlreadyExistsException when name is duplicate in program")
    void execute_withDuplicateName_throwsException() {
        when(programClassRepository.existsByNameInProgram(PROGRAM_ID, "Kids Beginner Monday")).thenReturn(true);

        CreateClassCommand command = new CreateClassCommand(
                TENANT_ID, PROGRAM_ID, "Kids Beginner Monday", ClassLevel.BEGINNER,
                ClassType.RECURRING, List.of(MONDAY_SCHEDULE), null, 20, CREATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ClassNameAlreadyExistsException.class);

        verify(programClassRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ---- T036: Optional professor assignment at creation ----

    @Test
    @DisplayName("should create class with professor assigned when professorId is provided")
    void execute_withProfessorId_createsClassWithProfessor() {
        UUID professorId = UUID.randomUUID();
        when(programClassRepository.existsByNameInProgram(PROGRAM_ID, "With Professor")).thenReturn(false);

        CreateClassCommand command = new CreateClassCommand(
                TENANT_ID, PROGRAM_ID, "With Professor", ClassLevel.INTERMEDIATE,
                ClassType.RECURRING, List.of(MONDAY_SCHEDULE), professorId, 20, CREATED_BY);

        ProgramClass result = service.execute(command);

        assertThat(result.getProfessorId()).isEqualTo(professorId);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ClassCreated event = (ClassCreated) eventCaptor.getValue();
        assertThat(event.professorId()).isEqualTo(professorId);
    }

    // ---- T049: One-time class happy path ----

    @Test
    @DisplayName("should create one-time class with specificDate and one schedule entry")
    void execute_withValidOneTimeCommand_createsOneTimeClass() {
        ClassScheduleEntry oneTimeEntry = new ClassScheduleEntry(
                null, java.time.LocalDate.now().plusDays(7), LocalTime.of(10, 0), LocalTime.of(12, 0));
        when(programClassRepository.existsByNameInProgram(PROGRAM_ID, "Workshop")).thenReturn(false);

        CreateClassCommand command = new CreateClassCommand(
                TENANT_ID, PROGRAM_ID, "Workshop", ClassLevel.ADVANCED,
                ClassType.ONE_TIME, List.of(oneTimeEntry), null, 15, CREATED_BY);

        ProgramClass result = service.execute(command);

        assertThat(result.getType()).isEqualTo(ClassType.ONE_TIME);
        assertThat(result.getScheduleEntries()).hasSize(1);
        assertThat(result.getScheduleEntries().get(0).specificDate()).isNotNull();

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ClassCreated.class);
    }

    // ---- T050: One-time class with past date ----

    @Test
    @DisplayName("should reject one-time class with past specificDate")
    void execute_oneTimeWithPastDate_throwsIllegalArgument() {
        ClassScheduleEntry pastEntry = new ClassScheduleEntry(
                null, java.time.LocalDate.now().minusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0));
        when(programClassRepository.existsByNameInProgram(PROGRAM_ID, "Past Workshop")).thenReturn(false);

        CreateClassCommand command = new CreateClassCommand(
                TENANT_ID, PROGRAM_ID, "Past Workshop", ClassLevel.BEGINNER,
                ClassType.ONE_TIME, List.of(pastEntry), null, 10, CREATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- T051: One-time class with missing specificDate ----

    @Test
    @DisplayName("should reject one-time class when schedule entry has dayOfWeek instead of specificDate")
    void execute_oneTimeWithDayOfWeek_throwsIllegalArgument() {
        when(programClassRepository.existsByNameInProgram(PROGRAM_ID, "Bad Workshop")).thenReturn(false);

        CreateClassCommand command = new CreateClassCommand(
                TENANT_ID, PROGRAM_ID, "Bad Workshop", ClassLevel.BEGINNER,
                ClassType.ONE_TIME, List.of(MONDAY_SCHEDULE), null, 10, CREATED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
