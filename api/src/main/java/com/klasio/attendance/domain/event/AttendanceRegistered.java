package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AttendanceRegistered(
        UUID registrationId,
        UUID sessionId,
        UUID tenantId,
        UUID classId,
        UUID studentId,
        UUID enrollmentId,
        UUID membershipId,
        String levelAtRegistration,
        int intendedHours,
        LocalDate sessionDate,
        LocalTime sessionStartTime,
        LocalTime sessionEndTime,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {}
