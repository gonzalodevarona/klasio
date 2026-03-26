package com.klasio.programclass.infrastructure.web;

import com.klasio.programclass.application.dto.ClassDetail;
import com.klasio.programclass.domain.model.ClassScheduleEntry;
import com.klasio.programclass.domain.model.ProgramClass;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ClassResponseDto {

    private ClassResponseDto() {
    }

    public record ClassDetailResponse(
            UUID id,
            UUID tenantId,
            UUID programId,
            String name,
            String level,
            String type,
            UUID professorId,
            String professorName,
            int maxStudents,
            String status,
            List<ScheduleEntryResponse> scheduleEntries,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy
    ) {
        public static ClassDetailResponse fromDomain(ProgramClass pc) {
            return new ClassDetailResponse(
                    pc.getId().value(),
                    pc.getTenantId(),
                    pc.getProgramId(),
                    pc.getName(),
                    pc.getLevel().name(),
                    pc.getType().name(),
                    pc.getProfessorId(),
                    null,
                    pc.getMaxStudents(),
                    pc.getStatus().name(),
                    pc.getScheduleEntries().stream()
                            .map(ScheduleEntryResponse::fromDomain)
                            .toList(),
                    pc.getCreatedAt(),
                    pc.getCreatedBy(),
                    pc.getUpdatedAt(),
                    pc.getUpdatedBy()
            );
        }

        public static ClassDetailResponse fromDetail(ClassDetail detail) {
            return new ClassDetailResponse(
                    detail.id(),
                    detail.tenantId(),
                    detail.programId(),
                    detail.name(),
                    detail.level(),
                    detail.type(),
                    detail.professorId(),
                    detail.professorName(),
                    detail.maxStudents(),
                    detail.status(),
                    detail.scheduleEntries().stream()
                            .map(ScheduleEntryResponse::fromDomain)
                            .toList(),
                    detail.createdAt(),
                    detail.createdBy(),
                    detail.updatedAt(),
                    detail.updatedBy()
            );
        }
    }

    public record ClassSummaryResponse(
            UUID id,
            UUID programId,
            String programName,
            String name,
            String level,
            String type,
            UUID professorId,
            String professorName,
            int maxStudents,
            String status,
            Instant createdAt
    ) {
        public static ClassSummaryResponse fromSummary(com.klasio.programclass.application.dto.ClassSummary summary) {
            return new ClassSummaryResponse(
                    summary.id(),
                    summary.programId(),
                    summary.programName(),
                    summary.name(),
                    summary.level(),
                    summary.type(),
                    summary.professorId(),
                    summary.professorName(),
                    summary.maxStudents(),
                    summary.status(),
                    summary.createdAt()
            );
        }
    }

    public record ScheduleEntryResponse(
            String dayOfWeek,
            String specificDate,
            String startTime,
            String endTime
    ) {
        public static ScheduleEntryResponse fromDomain(ClassScheduleEntry entry) {
            return new ScheduleEntryResponse(
                    entry.dayOfWeek() != null ? entry.dayOfWeek().name() : null,
                    entry.specificDate() != null ? entry.specificDate().toString() : null,
                    entry.startTime().toString(),
                    entry.endTime().toString()
            );
        }
    }
}
