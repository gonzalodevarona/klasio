package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Read model returned by ListClassSessionRosterUseCase.
 * Groups attendance registrations by session (date + time slot).
 */
public record ClassSessionRosterView(
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        List<RegistrantView> registrants
) {
    public record RegistrantView(
            UUID registrationId,
            UUID studentId,
            String studentName,
            String level,
            int intendedHours,
            String status
    ) {}
}
