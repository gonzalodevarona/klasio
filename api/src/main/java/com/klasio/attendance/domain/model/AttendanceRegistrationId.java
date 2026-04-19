package com.klasio.attendance.domain.model;

import java.util.UUID;

public record AttendanceRegistrationId(UUID value) {

    public AttendanceRegistrationId {
        if (value == null) throw new IllegalArgumentException("AttendanceRegistrationId must not be null");
    }

    public static AttendanceRegistrationId generate() {
        return new AttendanceRegistrationId(UUID.randomUUID());
    }

    public static AttendanceRegistrationId of(UUID id) {
        return new AttendanceRegistrationId(id);
    }
}
