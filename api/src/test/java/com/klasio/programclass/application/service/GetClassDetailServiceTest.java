package com.klasio.programclass.application.service;

import com.klasio.programclass.application.dto.ClassDetail;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassScheduleEntry;
import com.klasio.programclass.domain.model.ClassType;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetClassDetailServiceTest {

    @Mock
    private ProgramClassRepository programClassRepository;

    @Mock
    private ProfessorRepository professorRepository;

    private GetClassDetailService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();

    private static final ClassScheduleEntry MONDAY_SCHEDULE = new ClassScheduleEntry(
            DayOfWeek.MONDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0));

    private static final ClassScheduleEntry WEDNESDAY_SCHEDULE = new ClassScheduleEntry(
            DayOfWeek.WEDNESDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0));

    @BeforeEach
    void setUp() {
        service = new GetClassDetailService(programClassRepository, professorRepository);
    }

    // ---- T069: Happy path - returns full class detail with all fields ----

    @Test
    @DisplayName("should return class detail with all fields mapped from domain")
    void execute_withExistingClass_returnsClassDetail() {
        ProgramClass programClass = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, "Kids Beginner Monday", ClassLevel.BEGINNER,
                ClassType.RECURRING, List.of(MONDAY_SCHEDULE, WEDNESDAY_SCHEDULE),
                PROFESSOR_ID, 20, CREATED_BY);

        UUID classId = programClass.getId().value();

        when(programClassRepository.findById(TENANT_ID, classId))
                .thenReturn(Optional.of(programClass));
        when(professorRepository.findById(any(), any()))
                .thenReturn(Optional.empty());

        ClassDetail result = service.execute(TENANT_ID, classId);

        assertThat(result.id()).isEqualTo(classId);
        assertThat(result.tenantId()).isEqualTo(TENANT_ID);
        assertThat(result.programId()).isEqualTo(PROGRAM_ID);
        assertThat(result.name()).isEqualTo("Kids Beginner Monday");
        assertThat(result.level()).isEqualTo(ClassLevel.BEGINNER.name());
        assertThat(result.type()).isEqualTo(ClassType.RECURRING.name());
        assertThat(result.professorId()).isEqualTo(PROFESSOR_ID);
        assertThat(result.maxStudents()).isEqualTo(20);
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.scheduleEntries()).hasSize(2);
        assertThat(result.scheduleEntries()).containsExactly(MONDAY_SCHEDULE, WEDNESDAY_SCHEDULE);
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.createdBy()).isEqualTo(CREATED_BY);
        assertThat(result.updatedAt()).isNull();
        assertThat(result.updatedBy()).isNull();
    }

    // ---- T070: Class not found throws ClassNotFoundException ----

    @Test
    @DisplayName("should throw ClassNotFoundException when class does not exist")
    void execute_withNonExistentClass_throwsClassNotFoundException() {
        UUID classId = UUID.randomUUID();

        when(programClassRepository.findById(TENANT_ID, classId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, classId))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessage("Class with id '%s' not found".formatted(classId));
    }
}
