package com.klasio.student.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record StudentDeactivated(
        UUID studentId,
        UUID tenantId,
        UUID deactivatedBy,
        Instant occurredAt
) implements DomainEvent {}
