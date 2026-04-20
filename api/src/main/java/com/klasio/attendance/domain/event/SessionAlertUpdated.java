package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SessionAlertUpdated(
        UUID sessionId,
        UUID tenantId,
        UUID classId,
        String newReason,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        UUID actorId,
        String actorRole,
        Instant occurredAt
) implements DomainEvent {}
