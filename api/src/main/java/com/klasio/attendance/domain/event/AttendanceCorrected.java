package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AttendanceCorrected(
        UUID registrationId,
        UUID tenantId,
        UUID classId,
        UUID studentId,
        String previousStatus,
        String newStatus,
        String reason,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {}
