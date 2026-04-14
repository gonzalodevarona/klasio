package com.klasio.programclass.application.service;

import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.model.ProfessorId;
import com.klasio.professor.domain.model.ProfessorStatus;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.programclass.application.dto.AssignProfessorCommand;
import com.klasio.programclass.domain.event.ProfessorAssignedToClass;
import com.klasio.programclass.domain.event.ProfessorRemovedFromClass;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassScheduleEntry;
import com.klasio.programclass.domain.model.ClassType;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.ProfessorNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.DayOfWeek;
import java.time.Instant;
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
import com.klasio.shared.domain.model.IdentityDocumentType;

@ExtendWith(MockitoExtension.class)
class AssignProfessorServiceTest {

    @Mock
    private ProgramClassRepository programClassRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AssignProfessorService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID ASSIGNED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AssignProfessorService(programClassRepository, professorRepository, eventPublisher);
    }

    private ProgramClass createTestClass(UUID professorId) {
        ProgramClass pc = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, "Test Class", ClassLevel.BEGINNER, ClassType.RECURRING,
                List.of(new ClassScheduleEntry(DayOfWeek.MONDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0))),
                professorId, 20, ASSIGNED_BY);
        pc.clearDomainEvents();
        return pc;
    }

    private Professor createActiveProfessor(UUID professorId) {
        return Professor.reconstitute(
                ProfessorId.of(professorId), TENANT_ID, "Carlos", "Martinez",
                "carlos@example.com", null, ProfessorStatus.ACTIVE, null, null,
                Instant.now(), ASSIGNED_BY, null, null,
                com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678"
        );
    }

    // ---- T055: Happy path ----

    @Test
    @DisplayName("should assign professor to class and publish event")
    void execute_withActiveProfessor_assignsSuccessfully() {
        ProgramClass pc = createTestClass(null);
        when(programClassRepository.findById(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(pc));
        when(professorRepository.findById(TENANT_ID, PROFESSOR_ID)).thenReturn(Optional.of(createActiveProfessor(PROFESSOR_ID)));

        AssignProfessorCommand command = new AssignProfessorCommand(TENANT_ID, CLASS_ID, PROFESSOR_ID, ASSIGNED_BY);
        ProgramClass result = service.execute(command);

        assertThat(result.getProfessorId()).isEqualTo(PROFESSOR_ID);
        verify(programClassRepository).save(any());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ProfessorAssignedToClass.class);
    }

    // ---- T056: Reassignment ----

    @Test
    @DisplayName("should replace previous professor on reassignment and emit both events")
    void execute_reassignment_replacesPreviousProfessor() {
        UUID firstProfessor = UUID.randomUUID();
        UUID secondProfessor = UUID.randomUUID();
        ProgramClass pc = createTestClass(firstProfessor);
        when(programClassRepository.findById(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(pc));
        when(professorRepository.findById(TENANT_ID, secondProfessor)).thenReturn(Optional.of(createActiveProfessor(secondProfessor)));

        AssignProfessorCommand command = new AssignProfessorCommand(TENANT_ID, CLASS_ID, secondProfessor, ASSIGNED_BY);
        ProgramClass result = service.execute(command);

        assertThat(result.getProfessorId()).isEqualTo(secondProfessor);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeast(2)).publishEvent(eventCaptor.capture());

        List<Object> events = eventCaptor.getAllValues();
        assertThat(events.get(0)).isInstanceOf(ProfessorRemovedFromClass.class);
        assertThat(events.get(1)).isInstanceOf(ProfessorAssignedToClass.class);
    }

    // ---- T057: Deactivated professor rejected ----

    @Test
    @DisplayName("should reject assignment of deactivated professor")
    void execute_withDeactivatedProfessor_throwsIllegalArgument() {
        ProgramClass pc = createTestClass(null);
        Professor deactivated = Professor.reconstitute(
                ProfessorId.of(PROFESSOR_ID), TENANT_ID, "Carlos", "Martinez",
                "carlos@example.com", null, ProfessorStatus.DEACTIVATED, null, null,
                Instant.now(), ASSIGNED_BY, null, null,
                com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678"
        );

        when(programClassRepository.findById(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(pc));
        when(professorRepository.findById(TENANT_ID, PROFESSOR_ID)).thenReturn(Optional.of(deactivated));

        AssignProfessorCommand command = new AssignProfessorCommand(TENANT_ID, CLASS_ID, PROFESSOR_ID, ASSIGNED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalArgumentException.class);

        verify(programClassRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ---- T058: Professor not found ----

    @Test
    @DisplayName("should throw ProfessorNotFoundException when professor does not exist")
    void execute_withNonExistentProfessor_throwsProfessorNotFound() {
        ProgramClass pc = createTestClass(null);
        when(programClassRepository.findById(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(pc));
        when(professorRepository.findById(TENANT_ID, PROFESSOR_ID)).thenReturn(Optional.empty());

        AssignProfessorCommand command = new AssignProfessorCommand(TENANT_ID, CLASS_ID, PROFESSOR_ID, ASSIGNED_BY);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ProfessorNotFoundException.class);

        verify(programClassRepository, never()).save(any());
    }
}
