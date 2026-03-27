package com.klasio.student.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record StudentEnrolled(
        UUID enrollmentId,
        UUID tenantId,
        UUID studentId,
        UUID programId,
        String level,
        UUID createdBy,
        Instant occurredAt
) implements DomainEvent {}
