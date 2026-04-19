package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceMarkedAbsent(
        UUID registrationId,
        UUID sessionId,
        UUID tenantId,
        UUID classId,
        UUID studentId,
        LocalDate sessionDate,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {}
