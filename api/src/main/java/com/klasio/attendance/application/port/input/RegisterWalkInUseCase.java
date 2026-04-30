package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.RegisterWalkInCommand;
import com.klasio.attendance.domain.model.AttendanceRegistration;

public interface RegisterWalkInUseCase {
    AttendanceRegistration execute(RegisterWalkInCommand command);
}
