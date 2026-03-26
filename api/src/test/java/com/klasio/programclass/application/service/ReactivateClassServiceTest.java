package com.klasio.programclass.application.service;

import com.klasio.programclass.domain.event.ClassReactivated;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassScheduleEntry;
import com.klasio.programclass.domain.model.ClassStatus;
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
class ReactivateClassServiceTest {

    @Mock
    private ProgramClassRepository programClassRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReactivateClassService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();
    private static final UUID REACTIVATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ReactivateClassService(programClassRepository, eventPublisher);
    }

    private ProgramClass createInactiveClass() {
        ProgramClass pc = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, "Test Class", ClassLevel.BEGINNER, ClassType.RECURRING,
                List.of(new ClassScheduleEntry(DayOfWeek.MONDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0))),
                null, 20, CREATED_BY);
        pc.deactivate(CREATED_BY);
        pc.clearDomainEvents();
        return pc;
    }

    // ---- T105: Happy path - inactive class reactivated ----

    @Test
    @DisplayName("should reactivate inactive class and publish ClassReactivated event")
    void execute_withInactiveClass_reactivatesAndPublishesEvent() {
        ProgramClass pc = createInactiveClass();
        when(programClassRepository.findById(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(pc));

        ProgramClass result = service.execute(TENANT_ID, CLASS_ID, REACTIVATED_BY);

        assertThat(result.getStatus()).isEqualTo(ClassStatus.ACTIVE);
        verify(programClassRepository).save(any());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ClassReactivated.class);

        ClassReactivated event = (ClassReactivated) eventCaptor.getValue();
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.programId()).isEqualTo(PROGRAM_ID);
        assertThat(event.reactivatedBy()).isEqualTo(REACTIVATED_BY);
    }

    // ---- T106: Already active class ----

    @Test
    @DisplayName("should throw IllegalStateException when class is already active")
    void execute_withActiveClass_throwsIllegalStateException() {
        ProgramClass pc = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, "Test Class", ClassLevel.BEGINNER, ClassType.RECURRING,
                List.of(new ClassScheduleEntry(DayOfWeek.MONDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0))),
                null, 20, CREATED_BY);
        pc.clearDomainEvents();
        when(programClassRepository.findById(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(pc));

        assertThatThrownBy(() -> service.execute(TENANT_ID, CLASS_ID, REACTIVATED_BY))
                .isInstanceOf(IllegalStateException.class);
    }
}
