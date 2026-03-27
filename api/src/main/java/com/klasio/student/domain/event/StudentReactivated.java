package com.klasio.student.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record StudentReactivated(
        UUID studentId,
        UUID tenantId,
        UUID reactivatedBy,
        Instant occurredAt
) implements DomainEvent {}
