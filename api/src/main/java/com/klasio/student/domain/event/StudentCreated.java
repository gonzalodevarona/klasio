package com.klasio.student.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record StudentCreated(
        UUID studentId,
        UUID tenantId,
        String firstName,
        String lastName,
        String email,
        UUID createdBy,
        Instant occurredAt
) implements DomainEvent {}
