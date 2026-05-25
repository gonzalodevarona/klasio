package com.klasio.programclass.application.service;

import com.klasio.programclass.application.dto.ClassSummary;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListClassesServiceTest {

    @Mock
    private ProgramClassRepository programClassRepository;

    private ListClassesService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();

    private static final ClassScheduleEntry MONDAY_SCHEDULE = new ClassScheduleEntry(
            DayOfWeek.MONDAY, null, LocalTime.of(18, 0), LocalTime.of(20, 0), null);

    private static final ClassScheduleEntry WEDNESDAY_SCHEDULE = new ClassScheduleEntry(
            DayOfWeek.WEDNESDAY, null, LocalTime.of(10, 0), LocalTime.of(12, 0), null);

    @BeforeEach
    void setUp() {
        service = new ListClassesService(programClassRepository);
    }

    // ---- T067: Happy path - returns paginated list of classes for a program ----

    @Test
    @DisplayName("should return paginated list of class summaries for a program")
    void execute_withNoFilters_returnsPaginatedClassSummaries() {
        Pageable pageable = PageRequest.of(0, 20);

        ProgramClass beginnerClass = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, "Kids Beginner Monday", ClassLevel.BEGINNER,
                ClassType.RECURRING, List.of(MONDAY_SCHEDULE), null, 20, CREATED_BY);

        ProgramClass advancedClass = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, "Advanced Wednesday", ClassLevel.ADVANCED,
                ClassType.RECURRING, List.of(WEDNESDAY_SCHEDULE), UUID.randomUUID(), 15, CREATED_BY);

        Page<ProgramClass> repositoryPage = new PageImpl<>(
                List.of(beginnerClass, advancedClass), pageable, 2);

        when(programClassRepository.findByProgramId(TENANT_ID, PROGRAM_ID, pageable, null, null))
                .thenReturn(repositoryPage);

        Page<ClassSummary> result = service.execute(TENANT_ID, PROGRAM_ID, null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        ClassSummary first = result.getContent().get(0);
        assertThat(first.id()).isEqualTo(beginnerClass.getId().value());
        assertThat(first.programId()).isEqualTo(PROGRAM_ID);
        assertThat(first.name()).isEqualTo("Kids Beginner Monday");
        assertThat(first.level()).isEqualTo(ClassLevel.BEGINNER.name());
        assertThat(first.type()).isEqualTo(ClassType.RECURRING.name());
        assertThat(first.maxStudents()).isEqualTo(20);
        assertThat(first.status()).isEqualTo(ClassStatus.ACTIVE.name());
        assertThat(first.createdAt()).isNotNull();

        ClassSummary second = result.getContent().get(1);
        assertThat(second.id()).isEqualTo(advancedClass.getId().value());
        assertThat(second.name()).isEqualTo("Advanced Wednesday");
        assertThat(second.level()).isEqualTo(ClassLevel.ADVANCED.name());
        assertThat(second.professorId()).isNotNull();
        assertThat(second.maxStudents()).isEqualTo(15);

        verify(programClassRepository).findByProgramId(TENANT_ID, PROGRAM_ID, pageable, null, null);
    }

    // ---- T068: Filtering by level and status parameters ----

    @Test
    @DisplayName("should delegate level and status filters to repository and return filtered results")
    void execute_withLevelAndStatusFilters_returnFilteredResults() {
        Pageable pageable = PageRequest.of(0, 20);
        ClassLevel filterLevel = ClassLevel.INTERMEDIATE;
        ClassStatus filterStatus = ClassStatus.ACTIVE;

        ProgramClass intermediateClass = ProgramClass.create(
                TENANT_ID, PROGRAM_ID, "Intermediate Monday", ClassLevel.INTERMEDIATE,
                ClassType.RECURRING, List.of(MONDAY_SCHEDULE), null, 25, CREATED_BY);

        Page<ProgramClass> repositoryPage = new PageImpl<>(
                List.of(intermediateClass), pageable, 1);

        when(programClassRepository.findByProgramId(TENANT_ID, PROGRAM_ID, pageable, filterLevel, filterStatus))
                .thenReturn(repositoryPage);

        Page<ClassSummary> result = service.execute(TENANT_ID, PROGRAM_ID, filterLevel, filterStatus, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);

        ClassSummary summary = result.getContent().get(0);
        assertThat(summary.id()).isEqualTo(intermediateClass.getId().value());
        assertThat(summary.name()).isEqualTo("Intermediate Monday");
        assertThat(summary.level()).isEqualTo(ClassLevel.INTERMEDIATE.name());
        assertThat(summary.status()).isEqualTo(ClassStatus.ACTIVE.name());
        assertThat(summary.maxStudents()).isEqualTo(25);

        verify(programClassRepository).findByProgramId(TENANT_ID, PROGRAM_ID, pageable, filterLevel, filterStatus);
    }
}
