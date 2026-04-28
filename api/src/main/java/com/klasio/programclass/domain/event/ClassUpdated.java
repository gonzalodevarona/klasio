package com.klasio.programclass.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ClassUpdated(
        UUID classId,
        UUID tenantId,
        UUID programId,
        String name,
        String level,
        int maxStudents,
        UUID updatedBy,
        Instant occurredAt,
        /**
         * True when the schedule entries (day/time slots) were modified.
         * The ClassScheduleChangedListener uses this flag to decide whether to
         * cancel future registrations — a level-only change must not trigger that
         * cancellation, because the dedicated cascade service (RF-36) handles it.
         */
        boolean scheduleChanged
) implements DomainEvent {}
