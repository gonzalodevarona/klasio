package com.klasio.student.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record StudentPromoted(
        UUID enrollmentId,
        UUID tenantId,
        UUID studentId,
        UUID programId,
        String previousLevel,
        String newLevel,
        UUID changedBy,
        Instant occurredAt
) implements DomainEvent {}
