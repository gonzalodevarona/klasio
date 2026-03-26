package com.klasio.programclass.application.service;

import com.klasio.programclass.domain.event.ProfessorRemovedFromClass;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveProfessorServiceTest {

    @Mock
    private ProgramClassRepository programClassRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RemoveProfessorService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();
    private static final UUID REMOVED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RemoveProfessorService(programClassRepository, eventPublisher);
    }

    private ProgramClass createClassWithProfessor() {
        ProgramClass pc = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, "Test Class", ClassLevel.BEGINNER, ClassType.RECURRING,
                List.of(new ClassScheduleEntry(DayOfWeek.MONDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0))),
                PROFESSOR_ID, 20, CREATED_BY);
        pc.clearDomainEvents();
        return pc;
    }

    // ---- T114: Happy path - professor removed ----

    @Test
    @DisplayName("should remove professor from class and publish ProfessorRemovedFromClass event")
    void execute_withAssignedProfessor_removesAndPublishesEvent() {
        ProgramClass pc = createClassWithProfessor();
        when(programClassRepository.findById(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(pc));

        ProgramClass result = service.execute(TENANT_ID, CLASS_ID, REMOVED_BY);

        assertThat(result.getProfessorId()).isNull();
        verify(programClassRepository).save(any());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ProfessorRemovedFromClass.class);

        ProfessorRemovedFromClass event = (ProfessorRemovedFromClass) eventCaptor.getValue();
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.programId()).isEqualTo(PROGRAM_ID);
        assertThat(event.previousProfessorId()).isEqualTo(PROFESSOR_ID);
        assertThat(event.removedBy()).isEqualTo(REMOVED_BY);
    }

    // ---- T115: No professor assigned ----

    @Test
    @DisplayName("should throw IllegalStateException when no professor is assigned")
    void execute_withNoProfessorAssigned_throwsIllegalStateException() {
        ProgramClass pc = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, "Test Class", ClassLevel.BEGINNER, ClassType.RECURRING,
                List.of(new ClassScheduleEntry(DayOfWeek.MONDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0))),
                null, 20, CREATED_BY);
        pc.clearDomainEvents();
        when(programClassRepository.findById(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(pc));

        assertThatThrownBy(() -> service.execute(TENANT_ID, CLASS_ID, REMOVED_BY))
                .isInstanceOf(IllegalStateException.class);
    }
}
