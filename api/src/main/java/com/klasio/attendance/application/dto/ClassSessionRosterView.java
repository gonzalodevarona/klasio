package com.klasio.attendance.application.dto;

import java.math.BigDecimal;
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
        String location,
        String status,
        String alertReason,
        String cancellationReason,
        List<RegistrantView> registrants
) {
    public record RegistrantView(
            UUID registrationId,
            UUID studentId,
            String studentName,
            String level,
            Integer intendedHours,
            String status,
            UUID createdBy,           // null when viewer is PROFESSOR; non-null for ADMIN/SUPERADMIN/MANAGER
            UUID dropInAttendeeId,    // null for regular student registrations
            String dropInAttendeeName,
            String dropInAttendeePhone,
            BigDecimal dropInPaymentAmount
    ) {}
}
