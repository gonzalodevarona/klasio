package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.programclass.infrastructure.persistence.ClassScheduleEntryJpaEntity;
import com.klasio.programclass.infrastructure.persistence.ProgramClassJpaEntity;
import com.klasio.programclass.infrastructure.persistence.SpringDataProgramClassRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ClassDetailsAdapter implements ClassDetailsPort {

    private final SpringDataProgramClassRepository programClassRepository;

    public ClassDetailsAdapter(SpringDataProgramClassRepository programClassRepository) {
        this.programClassRepository = programClassRepository;
    }

    @Override
    public Optional<ClassRegistrationView> findForRegistration(UUID tenantId, UUID classId) {
        return programClassRepository.findByTenantIdAndId(tenantId, classId)
                .map(this::toView);
    }

    @Override
    public List<ClassRegistrationView> findActiveByProgramAndLevel(UUID tenantId, UUID programId, String level) {
        return programClassRepository
                .findByTenantIdAndProgramIdAndLevelAndStatusOrderByCreatedAtDesc(
                        tenantId, programId, level, "ACTIVE", PageRequest.of(0, 500))
                .getContent()
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public Optional<ClassSummaryView> findClassSummary(UUID tenantId, UUID classId) {
        return programClassRepository.findByTenantIdAndId(tenantId, classId)
                .map(e -> new ClassSummaryView(e.getId(), e.getProgramId(), e.getProfessorId()));
    }

    @Override
    public Optional<String> findClassName(UUID tenantId, UUID classId) {
        return programClassRepository.findByTenantIdAndId(tenantId, classId)
                .map(ProgramClassJpaEntity::getName);
    }

    private ClassRegistrationView toView(ProgramClassJpaEntity entity) {
        List<ScheduleEntryView> entries = entity.getScheduleEntries().stream()
                .map(e -> new ScheduleEntryView(
                        e.getDayOfWeek() != null ? DayOfWeek.valueOf(e.getDayOfWeek()) : null,
                        e.getSpecificDate(),
                        e.getStartTime(),
                        e.getEndTime()
                ))
                .toList();

        return new ClassRegistrationView(
                entity.getId(),
                entity.getProgramId(),
                entity.getProfessorId(),
                entity.getLevel(),
                entity.getStatus(),
                entity.getType(),
                entity.getMaxStudents(),
                entity.getName(),
                entries
        );
    }
}
