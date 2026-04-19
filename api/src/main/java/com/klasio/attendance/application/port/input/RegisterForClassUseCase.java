package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.RegisterForClassCommand;
import com.klasio.attendance.domain.model.AttendanceRegistration;

public interface RegisterForClassUseCase {
    AttendanceRegistration execute(RegisterForClassCommand command);
}
