package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.AttendanceRegistrationView;

import java.util.UUID;

public interface GetMyRegistrationUseCase {
    AttendanceRegistrationView execute(UUID tenantId, UUID studentId, UUID registrationId);
}
