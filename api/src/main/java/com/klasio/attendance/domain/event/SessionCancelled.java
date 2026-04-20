package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SessionCancelled(
        UUID sessionId,
        UUID tenantId,
        UUID classId,
        String reason,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        UUID actorId,
        String actorRole,
        List<UUID> affectedStudentIds,
        Instant occurredAt
) implements DomainEvent {}
