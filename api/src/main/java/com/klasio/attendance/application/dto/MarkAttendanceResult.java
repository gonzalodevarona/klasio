package com.klasio.attendance.application.dto;

import java.util.List;
import java.util.UUID;

public record MarkAttendanceResult(List<MarkedRegistration> results) {

    public record MarkedRegistration(
            UUID registrationId,
            String status,
            boolean noHoursWarning
    ) {}
}
