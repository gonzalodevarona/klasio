package com.klasio.student.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record StudentUpdated(
        UUID studentId,
        UUID tenantId,
        String firstName,
        String lastName,
        String email,
        UUID updatedBy,
        Instant occurredAt
) implements DomainEvent {}
